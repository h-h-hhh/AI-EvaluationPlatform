package com.example.codeeval.repository;

import com.example.codeeval.entity.CodeSubmission;
import com.example.codeeval.entity.EvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 评价结果Repository接口
 */
@Repository
public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, Long> {

    Optional<EvaluationResult> findBySubmission(CodeSubmission submission);

    Optional<EvaluationResult> findBySubmissionId(Long submissionId);

    List<EvaluationResult> findBySubmissionAssignmentId(Long assignmentId);

    List<EvaluationResult> findBySubmissionStudentId(Long studentId);

    @org.springframework.data.jpa.repository.Query("SELECT AVG(e.finalScore) FROM EvaluationResult e")
    Double averageScore();
    
    void deleteBySubmissionId(Long submissionId);
}
