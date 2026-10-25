package com.example.codeeval.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CodeQualityService {

    /**
     * 分析代码质量（模拟SonarQube分析）
     */
    public Map<String, Object> analyzeCodeQuality(String code, String language) {
        Map<String, Object> result = new HashMap<>();
        
        // 代码行数统计
        Map<String, Object> metrics = analyzeMetrics(code);
        result.put("metrics", metrics);
        
        // 代码问题检测
        List<Map<String, Object>> issues = detectIssues(code, language);
        result.put("issues", issues);
        result.put("issueCount", issues.size());
        
        // 严重程度统计
        Map<String, Integer> severityCounts = countBySeverity(issues);
        result.put("severityCounts", severityCounts);
        
        // 代码质量评分
        int qualityScore = calculateQualityScore(metrics, issues);
        result.put("qualityScore", qualityScore);
        
        // 技术债务估算（分钟）
        int technicalDebt = estimateTechnicalDebt(issues);
        result.put("technicalDebtMinutes", technicalDebt);
        
        // 代码重复度
        double duplication = detectDuplication(code);
        result.put("duplicationPercent", duplication);
        
        // 复杂度分析
        Map<String, Object> complexity = analyzeComplexity(code);
        result.put("complexity", complexity);
        
        return result;
    }

    /**
     * 分析代码指标
     */
    private Map<String, Object> analyzeMetrics(String code) {
        Map<String, Object> metrics = new HashMap<>();
        
        if (code == null || code.isEmpty()) {
            metrics.put("lines", 0);
            metrics.put("statements", 0);
            metrics.put("classes", 0);
            metrics.put("methods", 0);
            metrics.put("commentLines", 0);
            metrics.put("blankLines", 0);
            return metrics;
        }
        
        String[] lines = code.split("\n");
        int totalLines = lines.length;
        int commentLines = 0;
        int blankLines = 0;
        int statements = 0;
        int classes = 0;
        int methods = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                blankLines++;
            } else if (trimmed.startsWith("//") || trimmed.startsWith("/*") || 
                       trimmed.startsWith("*") || trimmed.startsWith("<!--")) {
                commentLines++;
            } else {
                statements++;
                if (trimmed.contains("class ") || trimmed.contains("struct ")) classes++;
                if (trimmed.contains("function ") || trimmed.contains("def ") ||
                    trimmed.contains("void ") || trimmed.matches(".*\\(.*\\)\\s*\\{?")) {
                    methods++;
                }
            }
        }
        
        metrics.put("lines", totalLines);
        metrics.put("statements", statements);
        metrics.put("classes", classes);
        metrics.put("methods", methods);
        metrics.put("commentLines", commentLines);
        metrics.put("blankLines", blankLines);
        metrics.put("effectiveLines", totalLines - blankLines);
        metrics.put("commentPercent", totalLines > 0 ? (double) commentLines / totalLines * 100 : 0);
        
        return metrics;
    }

    /**
     * 检测代码问题
     */
    private List<Map<String, Object>> detectIssues(String code, String language) {
        List<Map<String, Object>> issues = new ArrayList<>();
        
        // 检测常见代码问题
        
        // 1. 检测TODO/FIXME
        if (code.contains("TODO") || code.contains("FIXME")) {
            Map<String, Object> issue = createIssue(
                "todo_comment",
                "TODO或FIXME注释存在",
                "medium",
                "comment",
                "代码中存在未完成的TODO标记，请确认是否需要处理。"
            );
            issues.add(issue);
        }
        
        // 2. 检测调试打印语句
        if (code.contains("System.out.println") || code.contains("console.log") ||
            code.contains("print(") || code.contains("printf(")) {
            Map<String, Object> issue = createIssue(
                "debug_print",
                "调试打印语句存在",
                "minor",
                "misc",
                "发现调试用的打印语句，建议移除或使用日志框架。"
            );
            issues.add(issue);
        }
        
        // 3. 检测硬编码值
        if (code.matches(".*\\b\\d{4,}\\b.*")) {
            Map<String, Object> issue = createIssue(
                "magic_number",
                "存在硬编码数字",
                "minor",
                "readability",
                "建议将硬编码的数字定义为常量，提高代码可维护性。"
            );
            issues.add(issue);
        }
        
        // 4. 检测长方法
        String[] lines = code.split("\n");
        if (lines.length > 200) {
            Map<String, Object> issue = createIssue(
                "long_method",
                "方法过长",
                "major",
                "design",
                "代码超过200行，建议拆分为更小的方法，提高可读性和可维护性。"
            );
            issues.add(issue);
        }
        
        // 5. 检测深层嵌套
        int maxNesting = calculateMaxNesting(code);
        if (maxNesting > 4) {
            Map<String, Object> issue = createIssue(
                "deep_nesting",
                "嵌套过深",
                "major",
                "readability",
                "代码嵌套层数超过4层，建议使用提前返回或提取方法来减少嵌套。"
            );
            issues.add(issue);
        }
        
        // 6. 检测空行过多
        long emptyLineCount = Arrays.stream(lines).filter(String::isEmpty).count();
        if (emptyLineCount > lines.length * 0.3) {
            Map<String, Object> issue = createIssue(
                "too_many_blank_lines",
                "空行过多",
                "info",
                "formatting",
                "代码中空行比例过高，建议适当精简。"
            );
            issues.add(issue);
        }
        
        // 7. 检测重复代码（简单检测）
        if (hasDuplication(code)) {
            Map<String, Object> issue = createIssue(
                "code_duplication",
                "存在重复代码",
                "major",
                "duplication",
                "代码中存在重复片段，建议提取为公共方法。"
            );
            issues.add(issue);
        }
        
        // 8. 检测捕获所有异常
        if (code.contains("catch (Exception") || code.contains("catch(...)")) {
            Map<String, Object> issue = createIssue(
                "catch_all_exception",
                "捕获所有异常",
                "minor",
                "error_handling",
                "建议捕获特定类型的异常，而不是捕获所有异常。"
            );
            issues.add(issue);
        }
        
        // 9. 检测未使用的变量
        if (code.matches(".*\\bint\\s+\\w+\\s*;.*") || code.matches(".*\\bString\\s+\\w+\\s*;.*")) {
            // 简单检测，实际需要更复杂的分析
            Map<String, Object> issue = createIssue(
                "unused_variable",
                "可能存在未使用变量",
                "info",
                "best_practices",
                "建议检查是否存在未使用的变量。"
            );
            issues.add(issue);
        }
        
        return issues;
    }

    private Map<String, Object> createIssue(String ruleId, String message, String severity, 
                                            String category, String description) {
        Map<String, Object> issue = new HashMap<>();
        issue.put("ruleId", ruleId);
        issue.put("message", message);
        issue.put("severity", severity);
        issue.put("category", category);
        issue.put("description", description);
        return issue;
    }

    private Map<String, Integer> countBySeverity(List<Map<String, Object>> issues) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("blocker", 0);
        counts.put("critical", 0);
        counts.put("major", 0);
        counts.put("minor", 0);
        counts.put("info", 0);
        
        for (Map<String, Object> issue : issues) {
            String severity = (String) issue.get("severity");
            counts.put(severity, counts.getOrDefault(severity, 0) + 1);
        }
        
        return counts;
    }

    private int calculateQualityScore(Map<String, Object> metrics, List<Map<String, Object>> issues) {
        int score = 100;
        
        // 根据问题扣分
        for (Map<String, Object> issue : issues) {
            String severity = (String) issue.get("severity");
            switch (severity) {
                case "blocker": score -= 20; break;
                case "critical": score -= 10; break;
                case "major": score -= 5; break;
                case "minor": score -= 2; break;
                case "info": score -= 1; break;
            }
        }
        
        // 根据注释率调整
        double commentPercent = (double) metrics.getOrDefault("commentPercent", 0);
        if (commentPercent < 5) {
            score -= 5;
        } else if (commentPercent > 10 && commentPercent < 30) {
            score += 5;
        }
        
        return Math.max(Math.min(score, 100), 0);
    }

    private int estimateTechnicalDebt(List<Map<String, Object>> issues) {
        int debt = 0;
        
        for (Map<String, Object> issue : issues) {
            String severity = (String) issue.get("severity");
            switch (severity) {
                case "blocker": debt += 60; break;
                case "critical": debt += 30; break;
                case "major": debt += 15; break;
                case "minor": debt += 5; break;
                case "info": debt += 1; break;
            }
        }
        
        return debt;
    }

    private double detectDuplication(String code) {
        // 简单的重复检测，实际应用中需要更复杂的算法
        String[] lines = code.split("\n");
        Set<String> uniqueLines = new HashSet<>();
        int duplicateCount = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 20) { // 只检查有意义的行
                if (!uniqueLines.add(trimmed)) {
                    duplicateCount++;
                }
            }
        }
        
        return lines.length > 0 ? (double) duplicateCount / lines.length * 100 : 0;
    }

    private Map<String, Object> analyzeComplexity(String code) {
        Map<String, Object> complexity = new HashMap<>();
        
        // 计算圈复杂度
        int cyclomatic = calculateCyclomaticComplexity(code);
        complexity.put("cyclomatic", cyclomatic);
        
        // 认知复杂度（简化版）
        int cognitive = estimateCognitiveComplexity(code);
        complexity.put("cognitive", cognitive);
        
        // 复杂度评级
        String rating;
        if (cyclomatic <= 10) {
            rating = "低复杂度";
        } else if (cyclomatic <= 20) {
            rating = "中等复杂度";
        } else if (cyclomatic <= 50) {
            rating = "高复杂度";
        } else {
            rating = "非常高复杂度";
        }
        complexity.put("rating", rating);
        
        return complexity;
    }

    private int calculateCyclomaticComplexity(String code) {
        int complexity = 1;
        
        String[] keywords = {"if ", "else if ", "for ", "while ", "case ", 
                           "catch ", "&&", "||", "?"};
        
        for (String keyword : keywords) {
            int idx = 0;
            while ((idx = code.indexOf(keyword, idx)) != -1) {
                complexity++;
                idx += keyword.length();
            }
        }
        
        return complexity;
    }

    private int estimateCognitiveComplexity(String code) {
        // 简化的认知复杂度估算
        int cognitive = 0;
        int nestingLevel = 0;
        
        for (char c : code.toCharArray()) {
            if (c == '{') {
                nestingLevel++;
                cognitive += nestingLevel;
            } else if (c == '}') {
                nestingLevel--;
            }
        }
        
        return cognitive;
    }

    private int calculateMaxNesting(String code) {
        int maxNesting = 0;
        int currentNesting = 0;
        
        for (char c : code.toCharArray()) {
            if (c == '{') {
                currentNesting++;
                maxNesting = Math.max(maxNesting, currentNesting);
            } else if (c == '}') {
                currentNesting--;
            }
        }
        
        return maxNesting;
    }

    private boolean hasDuplication(String code) {
        // 简化的重复检测
        String[] lines = code.split("\n");
        Map<String, Integer> lineCount = new HashMap<>();
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 30) {
                int count = lineCount.getOrDefault(trimmed, 0);
                if (count >= 2) {
                    return true;
                }
                lineCount.put(trimmed, count + 1);
            }
        }
        
        return false;
    }
}
