package com.example.codeeval.service;

import com.example.codeeval.config.LlmConfig;
import com.example.codeeval.dto.EvaluationResultDTO;
import com.example.codeeval.dto.EvaluationStatusDTO;
import com.example.codeeval.entity.Assignment;
import com.example.codeeval.entity.CodeSubmission;
import com.example.codeeval.entity.Course;
import com.example.codeeval.entity.EvaluationResult;
import com.example.codeeval.repository.CodeSubmissionRepository;
import com.example.codeeval.repository.EvaluationResultRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评价服务类
 *
 * 采用"异步任务 + 前端轮询"模式，评价流程拆分为两步：
 * 1. createPendingEvaluation（同步，在 HTTP 请求线程执行）：
 *    负责幂等校验并创建/复用 PENDING 状态的评价记录，请求立即返回，不阻塞等待
 * 2. executeEvaluationAsync（异步，在 evaluationExecutor 线程池执行）：
 *    调用 DeepSeek LLM 完成真实代码评价（Phase 3：已替换 Phase 1 的随机分数占位）
 * 3. 前端通过 GET /api/evaluations/status/{submissionId} 轮询评价状态
 *
 * 双引擎设计：
 * - 已配置 DEEPSEEK_API_KEY → analyzeCodeStrict 严格模式，LLM 调用失败直接标 FAILED
 *   （避免"LLM 挂了但用户看到本地分数"的静默错评）
 * - 未配置 API Key → analyzeCode 本地启发式分析（确定性静态分析，非随机数），
 *   作为无 Key 的演示/降级模式，feedback 中会注明
 *
 * 重要说明：
 * - executeEvaluationAsync 必须由 Spring 代理调用（从 Controller 调用），
 *   同类内部自调用会导致 @Async 失效退化为同步执行
 * - 异步方法体内不使用类级 @Transactional：每次 repository.save() 自带独立事务，
 *   保证 PROCESSING 等中间状态对轮询接口立即可见；
 *   读取 LAZY 关联数据时使用 TransactionTemplate 开启短事务（快照模式）
 */
@Service
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final EvaluationResultRepository evaluationRepository;
    private final CodeSubmissionRepository submissionRepository;
    private final DeepSeekAnalysisService deepSeekAnalysisService;
    private final LlmConfig llmConfig;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public EvaluationService(EvaluationResultRepository evaluationRepository,
                             CodeSubmissionRepository submissionRepository,
                             DeepSeekAnalysisService deepSeekAnalysisService,
                             LlmConfig llmConfig,
                             ObjectMapper objectMapper,
                             TransactionTemplate transactionTemplate) {
        this.evaluationRepository = evaluationRepository;
        this.submissionRepository = submissionRepository;
        this.deepSeekAnalysisService = deepSeekAnalysisService;
        this.llmConfig = llmConfig;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 步骤一（同步）：受理评价请求，创建或复用评价记录
     *
     * 幂等规则：
     * - 已存在 PENDING / PROCESSING / COMPLETED 的记录 → 直接返回现状，不重复触发
     *   （防止用户重复点击导致重复评价、重复消耗 LLM 配额）
     * - 已存在 FAILED 的记录 → 重置为 PENDING 并清空错误信息（支持失败重试）
     * - 不存在记录 → 新建 PENDING 记录
     *
     * @param submissionId 代码提交ID
     * @return 受理后的评价实体（状态可能为 PENDING / PROCESSING / COMPLETED）
     * @throws IllegalArgumentException 提交记录不存在时抛出（全局异常处理器转为 400）
     */
    public EvaluationResult createPendingEvaluation(Long submissionId) {
        // 校验提交记录存在（同步阶段做，异步线程中不再重复查）
        CodeSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交记录不存在"));

        // 查询该提交是否已有评价记录（one-to-one 关系）
        EvaluationResult existing = evaluationRepository.findBySubmissionId(submissionId).orElse(null);

        if (existing != null) {
            if (EvaluationResult.STATUS_PENDING.equals(existing.getStatus())
                    || EvaluationResult.STATUS_PROCESSING.equals(existing.getStatus())
                    || EvaluationResult.STATUS_COMPLETED.equals(existing.getStatus())) {
                // 幂等：任务已在队列中/执行中/已完成，直接复用现有记录
                log.info("评价任务幂等命中 submissionId={}, status={}", submissionId, existing.getStatus());
                return existing;
            }
            // FAILED → 重试：重置状态为 PENDING，清空历史错误信息
            existing.setStatus(EvaluationResult.STATUS_PENDING);
            existing.setErrorMessage(null);
            return evaluationRepository.save(existing);
        }

        // 首次评价：创建 PENDING 记录，分数保持默认 0，由异步任务填充
        EvaluationResult evaluation = EvaluationResult.builder()
                .submission(submission)
                .status(EvaluationResult.STATUS_PENDING)
                .build();
        return evaluationRepository.save(evaluation);
    }

    /**
     * 步骤二（异步）：执行真实评价（DeepSeek LLM 或本地启发式分析）
     *
     * 状态流转：PENDING → PROCESSING → COMPLETED / FAILED
     * 每次状态变更都立即 save 落库，保证轮询接口能实时看到进度
     *
     * LAZY 关联处理：异步线程没有 Hibernate 会话，直接访问 submission.getAssignment()
     * 等 LAZY 代理会抛 LazyInitializationException，因此用 TransactionTemplate 在
     * 短事务内把需要的标量数据一次性读取成快照，事务外的评分/落库操作只用快照。
     *
     * @param evaluationId 评价记录ID（createPendingEvaluation 返回的记录）
     * @param submissionId 提交ID（显式传入，避免触碰 LAZY 代理）
     */
    @Async("evaluationExecutor")
    public void executeEvaluationAsync(Long evaluationId, Long submissionId) {
        try {
            EvaluationResult evaluation = evaluationRepository.findById(evaluationId)
                    .orElseThrow(() -> new IllegalArgumentException("评价记录不存在: " + evaluationId));

            // 置为 PROCESSING 并立即落库，前端轮询马上可见"评价中"
            evaluation.setStatus(EvaluationResult.STATUS_PROCESSING);
            evaluationRepository.save(evaluation);
            log.info("评价任务开始执行 evaluationId={}, submissionId={}", evaluationId, submissionId);

            // ===== 1. 短事务内读取评价所需的标量快照（规避 LAZY 代理问题） =====
            SubmissionSnapshot snapshot = transactionTemplate.execute(tx -> {
                CodeSubmission s = submissionRepository.findById(submissionId)
                        .orElseThrow(() -> new IllegalArgumentException("提交记录不存在: " + submissionId));
                Assignment a = s.getAssignment();       // LAZY 初始化（事务内，安全）
                Course c = a.getCourse();               // 课程名仅用于 LLM 上下文，可为空
                return new SubmissionSnapshot(
                        s.getCodeContent(),
                        s.getRepositoryUrl(),
                        a.getTitle(),
                        c != null ? c.getName() : "");
            });

            // ===== 2. 准备代码内容与上下文 =====
            // Git 提交可能没有粘贴代码正文，用仓库地址作为分析输入的补充说明
            String code = (snapshot.codeContent == null || snapshot.codeContent.isBlank())
                    ? "// 学生通过 Git 仓库提交代码，仓库地址：" + snapshot.repositoryUrl
                    : snapshot.codeContent;

            Map<String, Object> context = new HashMap<>();
            context.put("assignmentTitle", snapshot.assignmentTitle);
            context.put("courseName", snapshot.courseName);

            // ===== 3. 执行评价：有 API Key 走严格 LLM 模式，无 Key 走本地启发式分析 =====
            boolean llmEnabled = llmConfig.getApiKey() != null && !llmConfig.getApiKey().isBlank();
            String engine;
            Map<String, Object> analysis;
            if (llmEnabled) {
                // 严格模式：超时 / HTTP 错误 / JSON 解析失败都会抛异常 → 走外层 catch 标记 FAILED
                analysis = deepSeekAnalysisService.analyzeCodeStrict(code, context);
                engine = "deepseek";
            } else {
                // 无 Key 演示模式：本地静态启发式分析（确定性算法，非随机数）
                analysis = deepSeekAnalysisService.analyzeCode(code, context);
                engine = "local";
                log.warn("未配置 DEEPSEEK_API_KEY，评价任务 evaluationId={} 使用本地启发式分析", evaluationId);
            }

            // ===== 4. 解析 5 维分数并加权计算总分 =====
            @SuppressWarnings("unchecked")
            Map<String, Object> scores = (Map<String, Object>) analysis.getOrDefault("scores", new HashMap<>());

            double correctness = round1(scoreValue(scores, "correctness"));
            double quality = round1(scoreValue(scores, "quality"));
            double performance = round1(scoreValue(scores, "performance"));
            double security = round1(scoreValue(scores, "security"));
            double originality = round1(scoreValue(scores, "originality"));

            // 权重与 DeepSeek prompt 的评分模型一致：30/25/20/15/10
            double finalScore = round1(correctness * 0.3 + quality * 0.25
                    + performance * 0.2 + security * 0.15 + originality * 0.1);

            // ===== 5. 回填结果并置为 COMPLETED =====
            String summary = String.valueOf(analysis.getOrDefault("summary",
                    "本次评价未生成综合评语。"));

            evaluation.setCorrectnessScore(correctness);
            evaluation.setQualityScore(quality);
            evaluation.setPerformanceScore(performance);
            evaluation.setSecurityScore(security);
            evaluation.setOriginalityScore(originality);
            evaluation.setFinalScore(finalScore);
            // feedback 存综合评价；本地模式在开头注明引擎，避免被误认为 LLM 结论
            evaluation.setFeedback(("local".equals(engine) ? "【本地静态分析模式：未配置 DEEPSEEK_API_KEY】" : "")
                    + summary);
            // aiAnalysis 存完整分析 JSON（含各维度 comment、suggestions、engine 标记），
            // 供 DTO 层解析出 suggestions 与详情展示
            analysis.put("engine", engine);
            evaluation.setAiAnalysis(objectMapper.writeValueAsString(analysis));
            evaluation.setStatus(EvaluationResult.STATUS_COMPLETED);
            evaluation.setEvaluatedAt(LocalDateTime.now());
            evaluationRepository.save(evaluation);

            log.info("评价任务完成 evaluationId={}, engine={}, finalScore={}", evaluationId, engine, finalScore);
        } catch (Exception e) {
            // 任何异常（含 DeepSeek 超时/HTTP错误/解析失败）都不向外抛出——异步方法无法被
            // 调用方感知，统一落库为 FAILED，由前端轮询感知并展示重试入口
            log.error("评价任务执行失败 evaluationId={}", evaluationId, e);
            markFailed(evaluationId, e.getMessage());
        }
    }

    /**
     * 将评价记录标记为失败
     *
     * @param evaluationId 评价记录ID
     * @param reason       失败原因（写入 errorMessage，前端可见）
     */
    private void markFailed(Long evaluationId, String reason) {
        try {
            evaluationRepository.findById(evaluationId).ifPresent(evaluation -> {
                evaluation.setStatus(EvaluationResult.STATUS_FAILED);
                evaluation.setErrorMessage(reason);
                evaluationRepository.save(evaluation);
            });
        } catch (Exception ex) {
            // 兜底：标记失败本身失败（如数据库异常），仅记录日志，避免异步线程抛出未捕获异常
            log.error("标记评价任务失败状态时出错 evaluationId={}", evaluationId, ex);
        }
    }

    /**
     * 查询评价状态（供轮询接口使用）
     *
     * 只读取标量字段（不导航 LAZY 关联），无需事务注解
     *
     * @param submissionId 提交ID
     * @return 评价状态DTO；该提交尚未发起评价时返回 null（由 Controller 包装为 data=null）
     */
    public EvaluationStatusDTO getEvaluationStatus(Long submissionId) {
        return evaluationRepository.findBySubmissionId(submissionId)
                .map(e -> EvaluationStatusDTO.fromEntity(e, submissionId))
                .orElse(null);
    }

    // ==================== 查询方法（Phase 3 起返回 DTO） ====================
    //
    // 返回 DTO 需要导航 submission → assignment/course/student 的 LAZY 关联，
    // 因此标注 @Transactional(readOnly=true)：方法在整个 Hibernate 会话内执行，
    // LAZY 代理可安全初始化，同时只读事务不加写锁。
    // 控制器 → 服务是跨 Bean 调用，事务注解经代理正常生效。

    @Transactional(readOnly = true)
    public EvaluationResultDTO getEvaluationById(Long id) {
        return evaluationRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("评价结果不存在"));
    }

    @Transactional(readOnly = true)
    public EvaluationResultDTO getEvaluationBySubmission(Long submissionId) {
        return evaluationRepository.findBySubmissionId(submissionId)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("评价结果不存在"));
    }

    @Transactional(readOnly = true)
    public List<EvaluationResultDTO> getEvaluationsByAssignment(Long assignmentId) {
        return evaluationRepository.findBySubmissionAssignmentId(assignmentId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvaluationResultDTO> getEvaluationsByStudent(Long studentId) {
        return evaluationRepository.findBySubmissionStudentId(studentId).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * 实体 → DTO 转换并解析 suggestions
     * 必须在持久化上下文内调用（上述查询方法均已开启只读事务）
     */
    private EvaluationResultDTO toDTO(EvaluationResult e) {
        EvaluationResultDTO dto = EvaluationResultDTO.fromEntity(e);
        dto.setSuggestions(parseSuggestions(e.getAiAnalysis()));
        return dto;
    }

    /**
     * 从 aiAnalysis JSON 中解析改进建议列表
     * - DeepSeek 返回：suggestions 为字符串数组 → 直接取文本
     * - 本地分析返回：suggestions 为对象数组（含 type/priority/title/description）→ 取 description
     * 解析失败不影响主流程（返回空列表，feedback 中的综合评价仍可用）
     */
    private List<String> parseSuggestions(String aiAnalysis) {
        List<String> result = new ArrayList<>();
        if (aiAnalysis == null || aiAnalysis.isBlank()) {
            return result;
        }
        try {
            JsonNode root = objectMapper.readTree(aiAnalysis);
            JsonNode suggestions = root.path("suggestions");
            if (suggestions.isArray()) {
                for (JsonNode s : suggestions) {
                    if (s.isTextual()) {
                        result.add(s.asText());
                    } else if (s.isObject()) {
                        String desc = s.path("description").asText(s.path("title").asText(""));
                        if (!desc.isBlank()) {
                            result.add(desc);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析 aiAnalysis 中的 suggestions 失败，返回空列表: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 从分数 Map 中安全读取某一维度的数值（兼容 Integer/Double/String 类型）
     */
    private double scoreValue(Map<String, Object> scores, String key) {
        Object v = scores.get(key);
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        if (v instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                // 落到下面的默认值
            }
        }
        return 0.0;
    }

    /**
     * 评价所需的提交数据快照（在短事务内一次性读取，事务外只使用该不可变数据）
     */
    private record SubmissionSnapshot(String codeContent, String repositoryUrl,
                                      String assignmentTitle, String courseName) {
    }

    /**
     * 保留一位小数（分数展示友好）
     */
    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
