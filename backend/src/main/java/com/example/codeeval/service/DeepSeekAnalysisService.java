package com.example.codeeval.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DeepSeekAnalysisService {

    private final DeepSeekClient deepSeekClient;

    public DeepSeekAnalysisService(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    public Map<String, Object> analyzeCode(String code, Map<String, Object> context) {
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

        return result;
    }

    public Map<String, Object> analyzeCodeWithDeepSeek(String code, Map<String, Object> context) {
        if (deepSeekClient == null) {
            return analyzeCode(code, context);
        }

        try {
            String response = deepSeekClient.chat(buildSystemPrompt(), buildUserPrompt(code, context));
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> result = mapper.readValue(response, Map.class);

            Map<String, Integer> scores = calculateScoresFromDeepSeek(result);
            result.put("scores", scores);
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
            String response = deepSeekClient.chat(buildSystemPrompt(), buildUserPrompt(code, context));
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> result = mapper.readValue(response, Map.class);

            Map<String, Integer> scores = calculateScoresFromDeepSeek(result);
            result.put("scores", scores);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("DeepSeek 代码分析失败: " + e.getMessage(), e);
        }
    }

    /** 系统提示词：5 维评分模型 + 严格 JSON 输出格式（宽松/严格模式共用） */
    private String buildSystemPrompt() {
        return """
            你是一个专业的代码评审专家，专门分析Java代码质量。请按照以下维度分析代码：

            分析维度：
            1. 正确性：代码是否符合题目要求，有无逻辑错误、语法错误
            2. 代码质量：代码风格、命名规范、注释完整性、可读性、可维护性
            3. 性能优化：算法复杂度、内存使用、潜在瓶颈、时间复杂度
            4. 安全性：是否存在安全漏洞、SQL注入、敏感信息泄露、异常处理
            5. 创新点：是否有独特的解决思路、设计模式使用、代码复用性

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
            """;
    }

    /** 用户提示词：待分析代码 + 作业上下文（宽松/严格模式共用） */
    private String buildUserPrompt(String code, Map<String, Object> context) {
        return String.format("""
            请分析以下Java代码：

            【代码内容】
            %s

            【上下文信息】
            %s

            请严格按照JSON格式输出分析结果。
            """, code, context);
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