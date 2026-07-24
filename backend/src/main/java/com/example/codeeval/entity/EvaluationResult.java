package com.example.codeeval.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 评价结果实体类
 */
@Entity
@Table(name = "evaluation_results")
public class EvaluationResult {

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

    @Column(nullable = false)
    private Double correctnessScore = 0.0;

    @Column(nullable = false)
    private Double qualityScore = 0.0;

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
        this.originalityScore = 0.0;
        this.processScore = 0.0;
        this.finalScore = 0.0;
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

    public Double getCorrectnessScore() { return correctnessScore; }
    public void setCorrectnessScore(Double score) { this.correctnessScore = score; }

    public Double getQualityScore() { return qualityScore; }
    public void setQualityScore(Double score) { this.qualityScore = score; }

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
        private Double correctnessScore = 0.0, qualityScore = 0.0, originalityScore = 0.0, processScore = 0.0, finalScore = 0.0;
        private String feedback;
        private LocalDateTime evaluatedAt = LocalDateTime.now();

        public EvaluationResultBuilder submission(CodeSubmission s) { this.submission = s; return this; }
        public EvaluationResultBuilder codeQualityReport(String r) { this.codeQualityReport = r; return this; }
        public EvaluationResultBuilder testResults(String r) { this.testResults = r; return this; }
        public EvaluationResultBuilder similarityReport(String r) { this.similarityReport = r; return this; }
        public EvaluationResultBuilder aiAnalysis(String a) { this.aiAnalysis = a; return this; }
        public EvaluationResultBuilder correctnessScore(Double s) { this.correctnessScore = s; return this; }
        public EvaluationResultBuilder qualityScore(Double s) { this.qualityScore = s; return this; }
        public EvaluationResultBuilder originalityScore(Double s) { this.originalityScore = s; return this; }
        public EvaluationResultBuilder processScore(Double s) { this.processScore = s; return this; }
        public EvaluationResultBuilder finalScore(Double s) { this.finalScore = s; return this; }
        public EvaluationResultBuilder feedback(String f) { this.feedback = f; return this; }
        public EvaluationResultBuilder evaluatedAt(LocalDateTime t) { this.evaluatedAt = t; return this; }
        public EvaluationResult build() {
            EvaluationResult r = new EvaluationResult();
            r.setSubmission(submission); r.setCodeQualityReport(codeQualityReport); r.setTestResults(testResults);
            r.setSimilarityReport(similarityReport); r.setAiAnalysis(aiAnalysis);
            r.setCorrectnessScore(correctnessScore); r.setQualityScore(qualityScore); r.setOriginalityScore(originalityScore);
            r.setProcessScore(processScore); r.setFinalScore(finalScore); r.setFeedback(feedback); r.setEvaluatedAt(evaluatedAt);
            return r;
        }
    }
}
