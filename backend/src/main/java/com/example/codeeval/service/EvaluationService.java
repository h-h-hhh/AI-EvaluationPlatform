package com.example.codeeval.service;

import com.example.codeeval.entity.CodeSubmission;
import com.example.codeeval.entity.EvaluationResult;
import com.example.codeeval.repository.CodeSubmissionRepository;
import com.example.codeeval.repository.EvaluationResultRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价服务类
 */
@Service
public class EvaluationService {

    private final EvaluationResultRepository evaluationRepository;
    private final CodeSubmissionRepository submissionRepository;

    public EvaluationService(EvaluationResultRepository evaluationRepository,
                          CodeSubmissionRepository submissionRepository) {
        this.evaluationRepository = evaluationRepository;
        this.submissionRepository = submissionRepository;
    }

    public EvaluationResult evaluateSubmission(Long submissionId) {
        CodeSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交记录不存在"));

        EvaluationResult evaluation = EvaluationResult.builder()
                .submission(submission)
                .correctnessScore(evaluateCorrectness(submission))
                .qualityScore(evaluateQuality(submission))
                .originalityScore(evaluateOriginality(submission))
                .processScore(evaluateProcess(submission))
                .finalScore(calculateFinalScore(submission))
                .feedback(generateFeedback(submission))
                .evaluatedAt(LocalDateTime.now())
                .build();

        return evaluationRepository.save(evaluation);
    }

    private Double evaluateCorrectness(CodeSubmission submission) {
        return Math.random() * 100;
    }

    private Double evaluateQuality(CodeSubmission submission) {
        return Math.random() * 100;
    }

    private Double evaluateOriginality(CodeSubmission submission) {
        return Math.random() * 100;
    }

    private Double evaluateProcess(CodeSubmission submission) {
        return Math.random() * 100;
    }

    private Double calculateFinalScore(CodeSubmission submission) {
        double correctness = evaluateCorrectness(submission);
        double quality = evaluateQuality(submission);
        double originality = evaluateOriginality(submission);
        double process = evaluateProcess(submission);
        return (correctness * 0.3 + quality * 0.25 + originality * 0.25 + process * 0.2);
    }

    private String generateFeedback(CodeSubmission submission) {
        return "代码评价已完成，请查看详细报告。";
    }

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
}
