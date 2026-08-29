package com.example.codeeval.dto;

import com.example.codeeval.entity.EvaluationResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 评价结果 DTO（Phase 3）
 *
 * 取代原先直接返回 EvaluationResult 实体的做法：
 * - 实体的 submission 关联是 LAZY 的，脱离 Hibernate 会话后序列化会抛
 *   ByteBuddyInterceptor 错误（或意外触发 N+1 查询），DTO 化彻底消除该风险
 * - 将 submission/assignment/student 的关联信息拍平为标量字段，
 *   前端不再需要访问嵌套代理对象
 * - suggestions 从 aiAnalysis JSON 中解析为字符串列表，前端直接渲染
 *
 * 注意：fromEntity 内部会导航 LAZY 关联（submission → assignment/course/student），
 * 必须在持久化上下文内调用（EvaluationService 的查询方法已标注 @Transactional(readOnly=true)）
 */
public class EvaluationResultDTO {

    private Long id;
    private Long submissionId;
    private Long assignmentId;
    private String assignmentTitle;
    private String courseName;
    private Long studentId;
    private String studentName;

    /** 评价任务状态：PENDING / PROCESSING / COMPLETED / FAILED */
    private String status;
    /** 评价失败原因（仅 FAILED 时有值） */
    private String errorMessage;

    private Double correctnessScore;
    private Double qualityScore;
    private Double performanceScore;
    private Double securityScore;
    private Double originalityScore;
    private Double processScore;
    private Double finalScore;

    /** 综合评价文本（LLM 返回的 summary） */
    private String feedback;
    /** 完整 LLM 分析结果（原始 JSON 字符串，含各维度 comment） */
    private String aiAnalysis;
    /** 改进建议列表（从 aiAnalysis JSON 解析，前端直接渲染） */
    private List<String> suggestions = new ArrayList<>();

    /** 学生提交的代码内容（评价详情页"查看代码"使用） */
    private String codeContent;

    private LocalDateTime evaluatedAt;

    /**
     * 实体 → DTO 转换
     * 仅做字段平铺，不做 LAZY 之外的逻辑；suggestions 由服务层解析后 setSuggestions 填充
     */
    public static EvaluationResultDTO fromEntity(EvaluationResult e) {
        EvaluationResultDTO dto = new EvaluationResultDTO();
        dto.id = e.getId();
        dto.status = e.getStatus();
        dto.errorMessage = e.getErrorMessage();
        dto.correctnessScore = e.getCorrectnessScore();
        dto.qualityScore = e.getQualityScore();
        dto.performanceScore = e.getPerformanceScore();
        dto.securityScore = e.getSecurityScore();
        dto.originalityScore = e.getOriginalityScore();
        dto.processScore = e.getProcessScore();
        dto.finalScore = e.getFinalScore();
        dto.feedback = e.getFeedback();
        dto.aiAnalysis = e.getAiAnalysis();
        dto.evaluatedAt = e.getEvaluatedAt();

        // LAZY 关联导航：须在持久化上下文内执行（见类注释）
        if (e.getSubmission() != null) {
            dto.submissionId = e.getSubmission().getId();
            dto.codeContent = e.getSubmission().getCodeContent();

            if (e.getSubmission().getStudent() != null) {
                dto.studentId = e.getSubmission().getStudent().getId();
                dto.studentName = e.getSubmission().getStudent().getName();
            }
            if (e.getSubmission().getAssignment() != null) {
                dto.assignmentId = e.getSubmission().getAssignment().getId();
                dto.assignmentTitle = e.getSubmission().getAssignment().getTitle();
                if (e.getSubmission().getAssignment().getCourse() != null) {
                    dto.courseName = e.getSubmission().getAssignment().getCourse().getName();
                }
            }
        }
        return dto;
    }

    // Getters and Setters（Jackson 序列化使用）
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }

    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }

    public String getAssignmentTitle() { return assignmentTitle; }
    public void setAssignmentTitle(String assignmentTitle) { this.assignmentTitle = assignmentTitle; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Double getCorrectnessScore() { return correctnessScore; }
    public void setCorrectnessScore(Double correctnessScore) { this.correctnessScore = correctnessScore; }

    public Double getQualityScore() { return qualityScore; }
    public void setQualityScore(Double qualityScore) { this.qualityScore = qualityScore; }

    public Double getPerformanceScore() { return performanceScore; }
    public void setPerformanceScore(Double performanceScore) { this.performanceScore = performanceScore; }

    public Double getSecurityScore() { return securityScore; }
    public void setSecurityScore(Double securityScore) { this.securityScore = securityScore; }

    public Double getOriginalityScore() { return originalityScore; }
    public void setOriginalityScore(Double originalityScore) { this.originalityScore = originalityScore; }

    public Double getProcessScore() { return processScore; }
    public void setProcessScore(Double processScore) { this.processScore = processScore; }

    public Double getFinalScore() { return finalScore; }
    public void setFinalScore(Double finalScore) { this.finalScore = finalScore; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(String aiAnalysis) { this.aiAnalysis = aiAnalysis; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public String getCodeContent() { return codeContent; }
    public void setCodeContent(String codeContent) { this.codeContent = codeContent; }

    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
}
