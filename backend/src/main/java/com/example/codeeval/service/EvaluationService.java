package com.example.codeeval.service;

import com.example.codeeval.dto.EvaluationStatusDTO;
import com.example.codeeval.entity.CodeSubmission;
import com.example.codeeval.entity.EvaluationResult;
import com.example.codeeval.repository.CodeSubmissionRepository;
import com.example.codeeval.repository.EvaluationResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价服务类
 *
 * Phase 1 采用"异步任务 + 前端轮询"模式，评价流程拆分为两步：
 * 1. createPendingEvaluation（同步，在 HTTP 请求线程执行）：
 *    负责幂等校验并创建/复用 PENDING 状态的评价记录，请求立即返回，不阻塞等待
 * 2. executeEvaluationAsync（异步，在 evaluationExecutor 线程池执行）：
 *    执行真实评价逻辑（当前为随机分数占位，Phase 3 替换为 DeepSeek LLM 调用）
 * 3. 前端通过 GET /api/evaluations/status/{submissionId} 轮询评价状态
 *
 * 重要说明：
 * - executeEvaluationAsync 必须由 Spring 代理调用（从 Controller 调用），
 *   同类内部自调用会导致 @Async 失效退化为同步执行
 * - 异步方法体内不使用类级 @Transactional：每次 repository.save() 自带独立事务，
 *   保证 PROCESSING 等中间状态对轮询接口立即可见
 */
@Service
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final EvaluationResultRepository evaluationRepository;
    private final CodeSubmissionRepository submissionRepository;

    public EvaluationService(EvaluationResultRepository evaluationRepository,
                          CodeSubmissionRepository submissionRepository) {
        this.evaluationRepository = evaluationRepository;
        this.submissionRepository = submissionRepository;
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

        // 查询该提交是否已有评价记录（findOne-to-one 关系）
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
     * 步骤二（异步）：执行真实评价逻辑
     *
     * 状态流转：PENDING → PROCESSING → COMPLETED / FAILED
     * 每次状态变更都立即 save 落库，保证轮询接口能实时看到进度
     *
     * @param evaluationId 评价记录ID（createPendingEvaluation 返回的记录）
     * @param submissionId 提交ID（显式传入，避免在异步线程中触碰 LAZY 代理触发
     *                     LazyInitializationException）
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

            // ==================== 评价核心逻辑（Phase 1 占位实现） ====================
            // TODO(Phase 3): 此处替换为 DeepSeekAnalysisService.analyzeCodeWithDeepSeek，
            //  读取 submission 的代码内容调用 LLM，解析返回的 5 维 JSON 分数。
            //  submissionRepository.findById(submissionId) 可获取代码内容。

            // 模拟 LLM 调用耗时（真实 DeepSeek 耗时通常 10~60 秒），便于前端联调轮询展示
            Thread.sleep(3000);

            // 占位：随机生成 5 个维度的分数（0~100，保留 1 位小数）
            double correctness = round1(Math.random() * 100);
            double quality = round1(Math.random() * 100);
            double performance = round1(Math.random() * 100);
            double security = round1(Math.random() * 100);
            double originality = round1(Math.random() * 100);

            // 最终得分 = 加权求和，权重与 DeepSeekAnalysisService 的算法保持一致（30/25/20/15/10）
            double finalScore = round1(correctness * 0.3 + quality * 0.25
                    + performance * 0.2 + security * 0.15 + originality * 0.1);
            // =========================================================================

            // 回填分数并置为 COMPLETED
            evaluation.setCorrectnessScore(correctness);
            evaluation.setQualityScore(quality);
            evaluation.setPerformanceScore(performance);
            evaluation.setSecurityScore(security);
            evaluation.setOriginalityScore(originality);
            evaluation.setFinalScore(finalScore);
            evaluation.setFeedback("【占位数据】代码评价已完成，AI 详细分析将在后续版本接入。");
            evaluation.setStatus(EvaluationResult.STATUS_COMPLETED);
            evaluation.setEvaluatedAt(LocalDateTime.now());
            evaluationRepository.save(evaluation);

            log.info("评价任务完成 evaluationId={}, finalScore={}", evaluationId, finalScore);
        } catch (InterruptedException e) {
            // 线程被中断（如应用停机）：恢复中断标记并标记任务失败
            Thread.currentThread().interrupt();
            markFailed(evaluationId, "评价线程被中断: " + e.getMessage());
        } catch (Exception e) {
            // 任何异常（含 DeepSeek 调用失败）都不向外抛出——异步方法无法被调用方感知，
            // 统一落库为 FAILED，由前端轮询感知并展示重试入口
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
     * @param submissionId 提交ID
     * @return 评价状态DTO；该提交尚未发起评价时返回 null（由 Controller 包装为 data=null）
     */
    public EvaluationStatusDTO getEvaluationStatus(Long submissionId) {
        return evaluationRepository.findBySubmissionId(submissionId)
                .map(e -> EvaluationStatusDTO.fromEntity(e, submissionId))
                .orElse(null);
    }

    // ==================== 查询方法（保持原有功能不变） ====================

    public EvaluationResult getEvaluationById(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("评价结果不存在"));
    }

    public EvaluationResult getEvaluationBySubmission(Long submissionId) {
        return evaluationRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("评价结果不存在"));
    }

    public List<EvaluationResult> getEvaluationsByAssignment(Long assignmentId) {
        return evaluationRepository.findBySubmissionAssignmentId(assignmentId);
    }

    public List<EvaluationResult> getEvaluationsByStudent(Long studentId) {
        return evaluationRepository.findBySubmissionStudentId(studentId);
    }

    /**
     * 保留一位小数（分数展示友好）
     */
    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
