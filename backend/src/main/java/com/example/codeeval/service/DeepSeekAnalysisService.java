package com.example.codeeval.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DeepSeekAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekAnalysisService.class);

    private final DeepSeekClient deepSeekClient;

    public DeepSeekAnalysisService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    // ==================== 多语言识别（GitHub 提交支持 37 种扩展名，提示词不能写死 Java） ====================

    /** 源代码扩展名 → 编程语言展示名（用于 AI 提示词动态声明语言） */
    private static final Map<String, String> EXT_LANGUAGE;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("java", "Java");
        m.put("py", "Python");
        m.put("js", "JavaScript");
        m.put("mjs", "JavaScript");
        m.put("cjs", "JavaScript");
        m.put("jsx", "JavaScript (JSX/React)");
        m.put("ts", "TypeScript");
        m.put("tsx", "TypeScript (TSX/React)");
        m.put("cpp", "C++");
        m.put("cc", "C++");
        m.put("cxx", "C++");
        m.put("hpp", "C++");
        m.put("hh", "C++");
        m.put("c", "C");
        m.put("h", "C/C++");
        m.put("cs", "C#");
        m.put("go", "Go");
        m.put("rs", "Rust");
        m.put("kt", "Kotlin");
        m.put("kts", "Kotlin");
        m.put("scala", "Scala");
        m.put("rb", "Ruby");
        m.put("php", "PHP");
        m.put("swift", "Swift");
        m.put("m", "Objective-C");
        m.put("mm", "Objective-C++");
        m.put("dart", "Dart");
        m.put("vue", "Vue 单文件组件 (HTML/JS/CSS)");
        m.put("svelte", "Svelte");
        m.put("sh", "Shell");
        m.put("sql", "SQL");
        m.put("html", "HTML");
        m.put("css", "CSS");
        m.put("scss", "SCSS");
        m.put("less", "Less");
        EXT_LANGUAGE = Collections.unmodifiableMap(m);
    }

    /** 合并代码中文件段标记：===== FILE: path/to/file.ext ===== */
    private static final Pattern FILE_MARKER = Pattern.compile("=====\\s*FILE:\\s*(.+?)\\s*=====");

    /**
     * 识别待评价代码的主要编程语言。
     *
     * 判定顺序：
     * 1. GitHub 提交的合并代码含 "===== FILE: 路径 =====" 分段标记 → 按文件扩展名统计，
     *    取出现次数最多的语言为主语言；若第二语言文件数达到主语言一半，提示"多语言（含 X）"
     * 2. 无文件标记（历史粘贴数据/单文件内容）→ 按代码内容特征启发式判断
     * 3. 均不命中 → "通用编程语言"（提示词保持中立，不误导 AI）
     */
    String detectLanguage(String code) {
        if (code == null || code.isBlank()) {
            return "通用编程语言";
        }

        // ① 从 FILE 标记统计扩展名
        Matcher matcher = FILE_MARKER.matcher(code);
        Map<String, Integer> langCount = new HashMap<>();
        int fileCount = 0;
        while (matcher.find()) {
            fileCount++;
            String path = matcher.group(1);
            int dot = path.lastIndexOf('.');
            if (dot >= 0 && dot < path.length() - 1) {
                String ext = path.substring(dot + 1).toLowerCase();
                String lang = EXT_LANGUAGE.get(ext);
                if (lang != null) {
                    langCount.merge(lang, 1, Integer::sum);
                }
            }
        }
        if (!langCount.isEmpty()) {
            // 按文件数降序排列语言
            List<Map.Entry<String, Integer>> sorted = langCount.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .toList();
            String top = sorted.get(0).getKey();
            // 多语言仓库：次语言文件数 ≥ 主语言一半时才在提示词中提及，避免零星文件噪声
            if (sorted.size() > 1 && sorted.get(1).getValue() * 2 >= sorted.get(0).getValue()) {
                return top + " 为主的多语言代码（另含 " + sorted.get(1).getKey() + "）";
            }
            return top;
        }

        // ② 无文件标记：按内容特征启发式（历史粘贴数据走这里）
        return detectLanguageByContent(code);
    }

    /** 无文件分段标记时，按代码关键字特征猜测语言（优先级按特征唯一性排列） */
    private String detectLanguageByContent(String code) {
        if (code.contains("<?php")) return "PHP";
        if (code.contains("package main") || code.contains("func main(") || code.contains("fmt.")) return "Go";
        if (code.contains("fn main") || code.contains("println!") || code.contains("let mut ")) return "Rust";
        if (code.contains("using System;") || code.contains("Console.WriteLine") || code.contains("namespace ")) return "C#";
        if (code.contains("public class") || code.contains("System.out.print") || code.contains("import java.")) return "Java";
        if (code.contains("if __name__") || code.contains("def ") && code.contains("print(")) return "Python";
        if (code.contains("#include") || code.contains("std::") || code.contains("printf(")) return "C/C++";
        if (code.contains("console.log") || code.contains("=>") || code.contains("require(")
                || code.contains("function ") || code.contains("const ")) return "JavaScript/TypeScript";
        if (code.contains("puts ") || code.contains("def ") && code.contains("\nend")) return "Ruby";
        return "通用编程语言";
    }

    public Map<String, Object> analyzeCode(String code, Map<String, Object> context) {
        log.info("本地启发式评价，识别语言：{}", detectLanguage(code));
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> basicAnalysis = analyzeBasicMetrics(code);
        result.putAll(basicAnalysis);

        Map<String, Object> structureAnalysis = analyzeStructure(code);
        result.put("structure", structureAnalysis);

        String summary = generateSummary(code, basicAnalysis, structureAnalysis, context);
        result.put("summary", summary);

        List<Map<String, String>> suggestions = generateSuggestions(code, basicAnalysis, structureAnalysis);
        result.put("suggestions", suggestions);

        Map<String, Integer> scores = calculateScores(basicAnalysis, structureAnalysis);
        result.put("scores", scores);

        // 为 5 个评价维度生成确定性评语（details 结构），
        // 供 EvaluationService.parseDimensionComments 第 2 层读取，
        // 解决本地静态分析模式下学生端维度评语卡全为"-"的问题
        result.put("details", buildDimensionDetails(code, basicAnalysis, structureAnalysis, scores));

        return result;
    }

    /**
     * 基于本地静态分析指标，为每个维度生成一段确定性评语。
     * 结构与 DeepSeek 模式对齐：details.{dim}.comment，score 直接复用本地评分。
     */
    private Map<String, Object> buildDimensionDetails(String code,
                                                      Map<String, Object> basicMetrics,
                                                      Map<String, Object> structure,
                                                      Map<String, Integer> scores) {
        Map<String, Object> details = new HashMap<>();

        // 正确性：本地模式做静态语法检查（大括号配对 + 是否存在各语言常见程序入口）
        String correctnessComment;
        if (code == null || code.isBlank()) {
            correctnessComment = "未检测到代码内容，无法进行正确性分析。";
        } else {
            long open = code.chars().filter(c -> c == '{').count();
            long close = code.chars().filter(c -> c == '}').count();
            // 多语言入口特征：Java/C/C++/C# 的 main(、Go 的 func main、Rust 的 fn main、Python 的 __main__
            boolean hasEntry = code.contains("main(") || code.contains("func main")
                    || code.contains("fn main") || code.contains("__main__");
            if (open != close) {
                correctnessComment = "检测到大括号不配对（{ " + open + " 个，} " + close + " 个），代码可能存在语法错误，请检查。";
            } else if (!hasEntry) {
                correctnessComment = "未检测到程序入口（如 Java/C/C++ 的 main 函数、Go/Rust 的 main、Python 的 if __name__ == \"__main__\"），请确认代码是否符合题目要求。";
            } else {
                correctnessComment = "语法结构基本完整，括号配对正确，未发现明显语法错误。建议结合测试用例进一步验证逻辑正确性。";
            }
        }
        details.put("correctness", Map.of(
                "score", scores.getOrDefault("correctness", 0),
                "comment", correctnessComment));

        // 代码质量：命名分 + 注释率 + 结构分综合描述
        double namingScore = (double) basicMetrics.getOrDefault("namingScore", 0);
        double commentRatio = (double) basicMetrics.getOrDefault("commentRatio", 0);
        String qualityComment = String.format(
                "命名规范得分 %.0f 分，注释占比约 %.0f%%。%s%s",
                namingScore,
                commentRatio * 100,
                namingScore < 60 ? "建议使用更有意义的驼峰命名提升可读性。" : "命名整体清晰。",
                commentRatio < 0.05 ? "注释偏少，建议为关键逻辑补充说明。" : "注释覆盖良好。");
        details.put("quality", Map.of(
                "score", scores.getOrDefault("quality", 0),
                "comment", qualityComment));

        // 性能：复杂度 + 循环嵌套的静态观察
        int complexity = (int) basicMetrics.getOrDefault("complexity", 0);
        int loopCount = (int) structure.getOrDefault("loopCount", 0);
        String performanceComment = String.format(
                "圈复杂度约 %d，检测到 %d 处循环。%s",
                complexity, loopCount,
                complexity > 10 ? "复杂度偏高，存在嵌套循环时注意算法时间规模，建议优化为更高效的算法。"
                                : "复杂度处于合理范围，未发现明显的性能热点。");
        details.put("performance", Map.of(
                "score", scores.getOrDefault("performance", 0),
                "comment", performanceComment));

        // 安全性：调试打印 / 硬编码等静态特征
        String securityComment;
        if (code != null && (code.contains("System.out.print") || code.contains("System.err.print"))) {
            securityComment = "检测到调试用的标准输出语句，正式代码中建议移除或改用日志框架，避免信息泄露。";
        } else {
            securityComment = "未发现明显的调试输出或敏感信息硬编码，注意运行时异常处理与输入校验即可。";
        }
        details.put("security", Map.of(
                "score", scores.getOrDefault("security", 0),
                "comment", securityComment));

        // 创新点：类/函数组织方式的静态观察
        int classCount = (int) structure.getOrDefault("classCount", 0);
        int functionCount = (int) structure.getOrDefault("functionCount", 0);
        String originalityComment = String.format(
                "检测到 %d 个类、%d 个函数定义。%s",
                classCount, functionCount,
                functionCount >= 3 ? "代码有一定模块化拆分，鼓励尝试设计模式或更通用的抽象。"
                                   : "结构较简单，建议尝试用类/方法组织代码，提升复用性与可测试性。");
        details.put("originality", Map.of(
                "score", scores.getOrDefault("originality", 0),
                "comment", originalityComment));

        return details;
    }

    public Map<String, Object> analyzeCodeWithDeepSeek(String code, Map<String, Object> context) {
        if (deepSeekClient == null) {
            return analyzeCode(code, context);
        }

        try {
            // 动态识别语言，避免提示词写死 Java 误导 AI 对 Python/Go/JS 等仓库的评价
            String language = detectLanguage(code);
            log.info("DeepSeek 宽松模式评价，识别语言：{}", language);
            String response = deepSeekClient.chat(buildSystemPrompt(language), buildUserPrompt(code, context, language));
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> result = mapper.readValue(response, Map.class);

            Map<String, Integer> scores = calculateScoresFromDeepSeek(result);
            result.put("scores", scores);
            result.put("language", language);
            return result;
        } catch (Exception e) {
            // 宽松模式（供 AnalysisController 的即席分析接口使用）：
            // LLM 调用失败时降级为本地启发式分析，保证接口始终有返回
            return analyzeCode(code, context);
        }
    }

    /**
     * Phase 3 新增：严格模式的 DeepSeek 代码分析（供评价链路使用）
     *
     * 与 analyzeCodeWithDeepSeek 的区别：
     * - LLM 调用失败（超时 / HTTP 错误 / JSON 解析失败）时不做本地降级，
     *   而是向上抛出 RuntimeException，由 EvaluationService 捕获后把评价任务
     *   标记为 FAILED 并记录 errorMessage，让前端能明确感知失败并重试
     * - 这样避免了"LLM 挂了但用户看到的是本地启发式分数"的静默错评问题
     *
     * @throws RuntimeException DeepSeek 调用或结果解析失败时抛出
     */
    public Map<String, Object> analyzeCodeStrict(String code, Map<String, Object> context) {
        try {
            // 动态识别语言（GitHub 仓库可能是 Python/Go/JS/C++ 等任意语言），
            // 让 AI 以对应语言的惯例（入口写法、生态、命名规范）进行评审
            String language = detectLanguage(code);
            log.info("DeepSeek 严格模式评价，识别语言：{}", language);
            String response = deepSeekClient.chat(buildSystemPrompt(language), buildUserPrompt(code, context, language));
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> result = mapper.readValue(response, Map.class);

            Map<String, Integer> scores = calculateScoresFromDeepSeek(result);
            result.put("scores", scores);
            result.put("language", language);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("DeepSeek 代码分析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 系统提示词：5 维评分模型 + 严格 JSON 输出格式（宽松/严格模式共用）。
     * language 为动态识别出的编程语言展示名，避免写死 "Java" 误导多语言仓库评审。
     */
    private String buildSystemPrompt(String language) {
        return String.format("""
            你是一个专业的代码评审专家，专门分析 %s 代码质量。请按照以下维度分析代码：

            分析维度：
            1. 正确性：代码是否符合题目要求，有无逻辑错误、语法错误
            2. 代码质量：代码风格、命名规范、注释完整性、可读性、可维护性
            3. 性能优化：算法复杂度、内存使用、潜在瓶颈、时间复杂度
            4. 安全性：是否存在安全漏洞、SQL注入、敏感信息泄露、异常处理
            5. 创新点：是否有独特的解决思路、设计模式使用、代码复用性

            注意：代码可能由多个文件合并而成（以 "===== FILE: 路径 =====" 分隔），
            请按 %s 的语言惯例与生态进行评审；评语使用中文。

            输出格式：严格按照JSON格式输出
            {
              "correctness": {"score": 0-100, "comment": "详细描述"},
              "quality": {"score": 0-100, "comment": "详细描述"},
              "performance": {"score": 0-100, "comment": "详细描述"},
              "security": {"score": 0-100, "comment": "详细描述"},
              "originality": {"score": 0-100, "comment": "详细描述"},
              "summary": "综合评价（200字左右）",
              "suggestions": ["建议1", "建议2", "建议3", "建议4", "建议5"]
            }
            """, language, language);
    }

    /** 用户提示词：待分析代码 + 作业上下文（宽松/严格模式共用），语言动态注入 */
    private String buildUserPrompt(String code, Map<String, Object> context, String language) {
        return String.format("""
            请分析以下 %s 代码：

            【代码内容】
            %s

            【上下文信息】
            %s

            请严格按照JSON格式输出分析结果。
            """, language, code, context);
    }

    private Map<String, Object> analyzeBasicMetrics(String code) {
        Map<String, Object> metrics = new HashMap<>();

        if (code == null || code.isEmpty()) {
            metrics.put("linesOfCode", 0);
            metrics.put("effectiveLines", 0);
            metrics.put("commentLines", 0);
            metrics.put("blankLines", 0);
            metrics.put("complexity", 0);
            return metrics;
        }

        String[] lines = code.split("\n");
        int totalLines = lines.length;
        int commentLines = 0;
        int blankLines = 0;
        int effectiveLines = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                blankLines++;
            } else if (trimmed.startsWith("//") || trimmed.startsWith("/*") ||
                       trimmed.startsWith("*") || trimmed.startsWith("<!--")) {
                commentLines++;
            } else {
                effectiveLines++;
            }
        }

        metrics.put("linesOfCode", totalLines);
        metrics.put("effectiveLines", effectiveLines);
        metrics.put("commentLines", commentLines);
        metrics.put("blankLines", blankLines);
        metrics.put("commentRatio", totalLines > 0 ? (double) commentLines / totalLines : 0);

        int complexity = estimateComplexity(code);
        metrics.put("complexity", complexity);

        double namingScore = evaluateNaming(code);
        metrics.put("namingScore", namingScore);

        return metrics;
    }

    private Map<String, Object> analyzeStructure(String code) {
        Map<String, Object> structure = new HashMap<>();

        String[] lines = code.split("\n");
        int functionCount = 0;
        int classCount = 0;
        int ifCount = 0;
        int loopCount = 0;
        int forCount = 0;
        int whileCount = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains("function ") || trimmed.contains("def ") ||
                trimmed.contains("public void") || trimmed.contains("private void") ||
                trimmed.contains("void ")) {
                functionCount++;
            }
            if (trimmed.contains("class ") || trimmed.contains("struct ") ||
                trimmed.contains("interface ")) {
                classCount++;
            }
            if (trimmed.startsWith("if (")) ifCount++;
            if (trimmed.startsWith("for (")) forCount++;
            if (trimmed.startsWith("while (")) whileCount++;
            if (trimmed.startsWith("foreach") || trimmed.contains("forEach")) forCount++;
        }

        loopCount = forCount + whileCount;

        structure.put("functionCount", functionCount);
        structure.put("classCount", classCount);
        structure.put("ifCount", ifCount);
        structure.put("loopCount", loopCount);

        double structureScore = 50;
        if (classCount > 0) structureScore += 20;
        if (functionCount > 0 && functionCount <= 10) structureScore += 15;
        if (functionCount > 10) structureScore += 5;
        if (loopCount > 0 && loopCount <= 5) structureScore += 10;
        if (ifCount > 0 && ifCount <= 3) structureScore += 5;

        structure.put("structureScore", Math.min(structureScore, 100));

        return structure;
    }

    private String generateSummary(String code, Map<String, Object> basicMetrics,
                                   Map<String, Object> structure, Map<String, Object> context) {
        int linesOfCode = (int) basicMetrics.getOrDefault("linesOfCode", 0);
        double namingScore = (double) basicMetrics.getOrDefault("namingScore", 0);
        double structureScore = (double) structure.getOrDefault("structureScore", 0);

        StringBuilder summary = new StringBuilder();

        if (linesOfCode == 0) {
            return "代码为空，无法进行分析。";
        }

        summary.append("代码共 ").append(linesOfCode).append(" 行，");

        if (linesOfCode < 50) {
            summary.append("代码较为简洁，");
        } else if (linesOfCode > 200) {
            summary.append("代码规模较大，");
        } else {
            summary.append("代码规模适中，");
        }

        if (namingScore >= 80) {
            summary.append("命名规范程度较高，");
        } else if (namingScore >= 60) {
            summary.append("命名规范程度一般，");
        } else {
            summary.append("建议改进变量和函数的命名规范，");
        }

        if (structureScore >= 80) {
            summary.append("代码结构清晰，设计合理。");
        } else if (structureScore >= 60) {
            summary.append("代码结构基本合理，有一定优化空间。");
        } else {
            summary.append("代码结构有待优化，建议进行重构。");
        }

        String assignmentTitle = (String) context.getOrDefault("assignmentTitle", "");
        if (!assignmentTitle.isEmpty()) {
            summary.append(" 作业《").append(assignmentTitle).append("》");
        }

        return summary.toString();
    }

    private List<Map<String, String>> generateSuggestions(String code,
                                                           Map<String, Object> basicMetrics,
                                                           Map<String, Object> structure) {
        List<Map<String, String>> suggestions = new ArrayList<>();

        double namingScore = (double) basicMetrics.getOrDefault("namingScore", 0);
        double commentRatio = (double) basicMetrics.getOrDefault("commentRatio", 0);
        int complexity = (int) basicMetrics.getOrDefault("complexity", 0);
        double structureScore = (double) structure.getOrDefault("structureScore", 0);

        if (namingScore < 70) {
            Map<String, String> suggestion = new HashMap<>();
            suggestion.put("type", "naming");
            suggestion.put("priority", "medium");
            suggestion.put("title", "命名规范");
            suggestion.put("description", "建议使用更有意义的变量名和函数名，遵循驼峰命名法或下划线命名规范。");
            suggestions.add(suggestion);
        }

        if (commentRatio < 0.1 && (int) basicMetrics.getOrDefault("linesOfCode", 0) > 50) {
            Map<String, String> suggestion = new HashMap<>();
            suggestion.put("type", "comment");
            suggestion.put("priority", "low");
            suggestion.put("title", "注释不足");
            suggestion.put("description", "建议添加适当的代码注释，特别是复杂逻辑和公共接口处。");
            suggestions.add(suggestion);
        }

        if (complexity > 10) {
            Map<String, String> suggestion = new HashMap<>();
            suggestion.put("type", "complexity");
            suggestion.put("priority", "high");
            suggestion.put("title", "复杂度较高");
            suggestion.put("description", "代码圈复杂度过高，建议将复杂函数拆分为更小的函数，降低单个函数的复杂度。");
            suggestions.add(suggestion);
        }

        if (structureScore < 60) {
            Map<String, String> suggestion = new HashMap<>();
            suggestion.put("type", "structure");
            suggestion.put("priority", "high");
            suggestion.put("title", "结构优化");
            suggestion.put("description", "建议考虑使用类或模块来组织代码，提高代码的可维护性和可测试性。");
            suggestions.add(suggestion);
        }

        if (code.contains("System.out.println") || code.contains("console.log")) {
            Map<String, String> suggestion = new HashMap<>();
            suggestion.put("type", "debug");
            suggestion.put("priority", "medium");
            suggestion.put("title", "调试代码残留");
            suggestion.put("description", "发现调试用的打印语句，建议在提交前移除或使用日志框架替代。");
            suggestions.add(suggestion);
        }

        if (code.contains("TODO") || code.contains("FIXME")) {
            Map<String, String> suggestion = new HashMap<>();
            suggestion.put("type", "todo");
            suggestion.put("priority", "low");
            suggestion.put("title", "待办事项");
            suggestion.put("description", "代码中存在TODO或FIXME标记，请确认是否需要处理。");
            suggestions.add(suggestion);
        }

        return suggestions;
    }

    private Map<String, Integer> calculateScores(Map<String, Object> basicMetrics,
                                                 Map<String, Object> structure) {
        Map<String, Integer> scores = new HashMap<>();

        scores.put("correctness", 85);

        double namingScore = (double) basicMetrics.getOrDefault("namingScore", 0);
        double structureScore = (double) structure.getOrDefault("structureScore", 0);
        double commentRatio = (double) basicMetrics.getOrDefault("commentRatio", 0);
        int quality = (int) ((namingScore * 0.3 + structureScore * 0.4 + commentRatio * 100 * 0.3));
        scores.put("quality", Math.min(quality, 100));

        int originality = estimateOriginality(basicMetrics, structure);
        scores.put("originality", originality);

        // Phase 3：本地模式补齐 performance / security 两个维度，
        // 保证与 DeepSeek 5 维模型结构一致（EvaluationService 统一按 5 维加权算总分）
        // - performance：本地静态分析无法测真实性能，用结构分近似（结构越清晰越利于性能优化）
        // - security：本地静态分析无法可靠检测安全漏洞，给中性偏保守的默认分
        scores.put("performance", Math.min(quality, 100));
        scores.put("security", 80);

        scores.put("process", 80);

        return scores;
    }

    private Map<String, Integer> calculateScoresFromDeepSeek(Map<String, Object> result) {
        Map<String, Integer> scores = new HashMap<>();

        try {
            Map<String, Object> correctness = (Map<String, Object>) result.get("correctness");
            Map<String, Object> quality = (Map<String, Object>) result.get("quality");
            Map<String, Object> performance = (Map<String, Object>) result.get("performance");
            Map<String, Object> security = (Map<String, Object>) result.get("security");
            Map<String, Object> originality = (Map<String, Object>) result.get("originality");

            scores.put("correctness", getScore(correctness));
            scores.put("quality", getScore(quality));
            scores.put("performance", getScore(performance));
            scores.put("security", getScore(security));
            scores.put("originality", getScore(originality));

            int finalScore = (scores.get("correctness") * 30 +
                             scores.get("quality") * 25 +
                             scores.get("performance") * 20 +
                             scores.get("security") * 15 +
                             scores.get("originality") * 10) / 100;
            scores.put("final", finalScore);

        } catch (Exception e) {
            scores.put("correctness", 0);
            scores.put("quality", 0);
            scores.put("performance", 0);
            scores.put("security", 0);
            scores.put("originality", 0);
            scores.put("final", 0);
        }

        return scores;
    }

    private int getScore(Map<String, Object> dimension) {
        if (dimension == null) return 0;
        Object scoreObj = dimension.get("score");
        if (scoreObj instanceof Number) {
            return ((Number) scoreObj).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(scoreObj));
        } catch (Exception e) {
            return 0;
        }
    }

    private int estimateComplexity(String code) {
        int complexity = 1;

        complexity += countOccurrences(code, "if ");
        complexity += countOccurrences(code, "else if ");
        complexity += countOccurrences(code, "for ");
        complexity += countOccurrences(code, "while ");
        complexity += countOccurrences(code, "case ");
        complexity += countOccurrences(code, "catch ");
        complexity += countOccurrences(code, "&&");
        complexity += countOccurrences(code, "||");
        complexity += countOccurrences(code, "?");

        return complexity;
    }

    private double evaluateNaming(String code) {
        double score = 75;

        if (code.matches(".*\\b[a-z]\\b.*")) {
            score -= 10;
        }

        if (code.contains("calculate") || code.contains("compute") ||
            code.contains("process") || code.contains("handle")) {
            score += 10;
        }

        return Math.min(Math.max(score, 0), 100);
    }

    private int estimateOriginality(Map<String, Object> basicMetrics, Map<String, Object> structure) {
        double structureScore = (double) structure.getOrDefault("structureScore", 0);

        int originality = 70;

        if (structureScore > 80) {
            originality += 15;
        } else if (structureScore > 60) {
            originality += 10;
        } else {
            originality -= 10;
        }

        int functionCount = (int) structure.getOrDefault("functionCount", 0);
        if (functionCount >= 3 && functionCount <= 10) {
            originality += 10;
        }

        return Math.min(Math.max(originality, 0), 100);
    }

    private int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}