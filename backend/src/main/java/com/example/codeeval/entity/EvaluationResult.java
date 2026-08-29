package com.example.codeeval.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 评价结果实体类
 *
 * Phase 1 变更：新增评价任务状态字段，支持"异步任务 + 前端轮询"模式
 * - status：评价任务生命周期状态（PENDING → PROCESSING → COMPLETED / FAILED）
 * - errorMessage：评价失败原因（仅 FAILED 时有值）
 * - performanceScore / securityScore：补齐与 DeepSeek 5 维评分模型对齐的两个维度
 */
@Entity
@Table(name = "evaluation_results")
public class EvaluationResult {

    /** 评价任务状态常量：待处理（已受理，尚未开始执行） */
    public static final String STATUS_PENDING = "PENDING";
    /** 评价任务状态常量：评价中（LLM 正在分析代码） */
    public static final String STATUS_PROCESSING = "PROCESSING";
    /** 评价任务状态常量：已完成（分数已落库，可查询结果） */
    public static final String STATUS_COMPLETED = "COMPLETED";
    /** 评价任务状态常量：失败（errorMessage 记录原因，可重试） */
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private CodeSubmission submission;

    @Column(columnDefinition = "TEXT")
    private String codeQualityReport;

    @Column(columnDefinition = "TEXT")
    private String testResults;

    @Column(columnDefinition = "TEXT")
    private String similarityReport;

    @Column(columnDefinition = "TEXT")
    private String aiAnalysis;

    /**
     * 评价任务状态：PENDING / PROCESSING / COMPLETED / FAILED
     * 默认 PENDING：记录创建后由异步线程池接管执行
     */
    @Column(nullable = false, length = 20)
    private String status = STATUS_PENDING;

    /**
     * 评价失败时的错误信息（仅 status = FAILED 时有值）
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Double correctnessScore = 0.0;

    @Column(nullable = false)
    private Double qualityScore = 0.0;

    /**
     * 性能维度得分（0~100），与 DeepSeek 分析 Prompt 的 performance 维度对齐
     */
    @Column(nullable = false)
    private Double performanceScore = 0.0;

    /**
     * 安全维度得分（0~100），与 DeepSeek 分析 Prompt 的 security 维度对齐
     */
    @Column(nullable = false)
    private Double securityScore = 0.0;

    @Column(nullable = false)
    private Double originalityScore = 0.0;

    @Column(nullable = false)
    private Double processScore = 0.0;

    @Column(nullable = false)
    private Double finalScore = 0.0;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(nullable = false)
    private LocalDateTime evaluatedAt = LocalDateTime.now();

    public EvaluationResult() {
        this.correctnessScore = 0.0;
        this.qualityScore = 0.0;
        this.performanceScore = 0.0;
        this.securityScore = 0.0;
        this.originalityScore = 0.0;
        this.processScore = 0.0;
        this.finalScore = 0.0;
        this.status = STATUS_PENDING;
        this.evaluatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CodeSubmission getSubmission() { return submission; }
    public void setSubmission(CodeSubmission submission) { this.submission = submission; }

    public String getCodeQualityReport() { return codeQualityReport; }
    public void setCodeQualityReport(String report) { this.codeQualityReport = report; }

    public String getTestResults() { return testResults; }
    public void setTestResults(String results) { this.testResults = results; }

    public String getSimilarityReport() { return similarityReport; }
    public void setSimilarityReport(String report) { this.similarityReport = report; }

    public String getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(String analysis) { this.aiAnalysis = analysis; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Double getCorrectnessScore() { return correctnessScore; }
    public void setCorrectnessScore(Double score) { this.correctnessScore = score; }

    public Double getQualityScore() { return qualityScore; }
    public void setQualityScore(Double score) { this.qualityScore = score; }

    public Double getPerformanceScore() { return performanceScore; }
    public void setPerformanceScore(Double score) { this.performanceScore = score; }

    public Double getSecurityScore() { return securityScore; }
    public void setSecurityScore(Double score) { this.securityScore = score; }

    public Double getOriginalityScore() { return originalityScore; }
    public void setOriginalityScore(Double score) { this.originalityScore = score; }

    public Double getProcessScore() { return processScore; }
    public void setProcessScore(Double score) { this.processScore = score; }

    public Double getFinalScore() { return finalScore; }
    public void setFinalScore(Double score) { this.finalScore = score; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime time) { this.evaluatedAt = time; }

    public static EvaluationResultBuilder builder() { return new EvaluationResultBuilder(); }

    public static class EvaluationResultBuilder {
        private CodeSubmission submission;
        private String codeQualityReport, testResults, similarityReport, aiAnalysis;
        private String status = STATUS_PENDING;
        private String errorMessage;
        private Double correctnessScore = 0.0, qualityScore = 0.0, performanceScore = 0.0, securityScore = 0.0,
                originalityScore = 0.0, processScore = 0.0, finalScore = 0.0;
        private String feedback;
        private LocalDateTime evaluatedAt = LocalDateTime.now();

        public EvaluationResultBuilder submission(CodeSubmission s) { this.submission = s; return this; }
        public EvaluationResultBuilder codeQualityReport(String r) { this.codeQualityReport = r; return this; }
        public EvaluationResultBuilder testResults(String r) { this.testResults = r; return this; }
        public EvaluationResultBuilder similarityReport(String r) { this.similarityReport = r; return this; }
        public EvaluationResultBuilder aiAnalysis(String a) { this.aiAnalysis = a; return this; }
        public EvaluationResultBuilder status(String s) { this.status = s; return this; }
        public EvaluationResultBuilder errorMessage(String m) { this.errorMessage = m; return this; }
        public EvaluationResultBuilder correctnessScore(Double s) { this.correctnessScore = s; return this; }
        public EvaluationResultBuilder qualityScore(Double s) { this.qualityScore = s; return this; }
        public EvaluationResultBuilder performanceScore(Double s) { this.performanceScore = s; return this; }
        public EvaluationResultBuilder securityScore(Double s) { this.securityScore = s; return this; }
        public EvaluationResultBuilder originalityScore(Double s) { this.originalityScore = s; return this; }
        public EvaluationResultBuilder processScore(Double s) { this.processScore = s; return this; }
        public EvaluationResultBuilder finalScore(Double s) { this.finalScore = s; return this; }
        public EvaluationResultBuilder feedback(String f) { this.feedback = f; return this; }
        public EvaluationResultBuilder evaluatedAt(LocalDateTime t) { this.evaluatedAt = t; return this; }
        public EvaluationResult build() {
            EvaluationResult r = new EvaluationResult();
            r.setSubmission(submission); r.setCodeQualityReport(codeQualityReport); r.setTestResults(testResults);
            r.setSimilarityReport(similarityReport); r.setAiAnalysis(aiAnalysis);
            r.setStatus(status); r.setErrorMessage(errorMessage);
            r.setCorrectnessScore(correctnessScore); r.setQualityScore(qualityScore);
            r.setPerformanceScore(performanceScore); r.setSecurityScore(securityScore);
            r.setOriginalityScore(originalityScore);
            r.setProcessScore(processScore); r.setFinalScore(finalScore); r.setFeedback(feedback); r.setEvaluatedAt(evaluatedAt);
            return r;
        }
    }
}
