package com.example.codeeval.dto;

import com.example.codeeval.entity.EvaluationResult;

import java.time.LocalDateTime;

/**
 * 评价任务状态DTO
 *
 * 用途：供前端轮询 GET /api/evaluations/status/{submissionId} 使用
 * 设计说明：
 * - 不直接返回 EvaluationResult 实体，因为实体的 submission 字段是 LAZY 关联，
 *   序列化时可能触发 ByteBuddyInterceptor 异常（项目已有此教训）
 * - finalScore 仅在 status = COMPLETED 时有业务意义
 * - errorMessage 仅在 status = FAILED 时有值，前端据此展示失败原因并提供重试入口
 */
public class EvaluationStatusDTO {

    /** 评价记录ID */
    private Long evaluationId;

    /** 关联的代码提交ID（由调用方传入，避免触碰 LAZY 代理） */
    private Long submissionId;

    /** 评价任务状态：PENDING / PROCESSING / COMPLETED / FAILED */
    private String status;

    /** 最终得分（仅 COMPLETED 时有值） */
    private Double finalScore;

    /** 失败原因（仅 FAILED 时有值） */
    private String errorMessage;

    /** 评价完成时间（仅 COMPLETED 时有值） */
    private LocalDateTime evaluatedAt;

    /**
     * 从评价实体构建状态DTO
     *
     * @param evaluation  评价实体（只读取基础列，不触碰 LAZY 的 submission 代理）
     * @param submissionId 提交ID，由 Controller/Service 显式传入
     */
    public static EvaluationStatusDTO fromEntity(EvaluationResult evaluation, Long submissionId) {
        EvaluationStatusDTO dto = new EvaluationStatusDTO();
        dto.setEvaluationId(evaluation.getId());
        dto.setSubmissionId(submissionId);
        dto.setStatus(evaluation.getStatus());
        dto.setFinalScore(evaluation.getFinalScore());
        dto.setErrorMessage(evaluation.getErrorMessage());
        dto.setEvaluatedAt(evaluation.getEvaluatedAt());
        return dto;
    }

    // Getters and Setters（Jackson 序列化依赖）
    public Long getEvaluationId() { return evaluationId; }
    public void setEvaluationId(Long evaluationId) { this.evaluationId = evaluationId; }

    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getFinalScore() { return finalScore; }
    public void setFinalScore(Double finalScore) { this.finalScore = finalScore; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
}
