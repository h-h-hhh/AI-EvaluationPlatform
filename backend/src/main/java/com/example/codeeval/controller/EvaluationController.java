package com.example.codeeval.controller;

import com.example.codeeval.dto.ApiResponse;
import com.example.codeeval.entity.EvaluationResult;
import com.example.codeeval.service.EvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评价控制器
 */
@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EvaluationResult>> getEvaluationById(@PathVariable Long id) {
        EvaluationResult evaluation = evaluationService.getEvaluationById(id);
        return ResponseEntity.ok(ApiResponse.success("查询成功", evaluation));
    }

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<ApiResponse<EvaluationResult>> getEvaluationBySubmission(@PathVariable Long submissionId) {
        EvaluationResult evaluation = evaluationService.getEvaluationBySubmission(submissionId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", evaluation));
    }

    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<ApiResponse<List<EvaluationResult>>> getEvaluationsByAssignment(@PathVariable Long assignmentId) {
        List<EvaluationResult> evaluations = evaluationService.getEvaluationsByAssignment(assignmentId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", evaluations));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<EvaluationResult>>> getEvaluationsByStudent(@PathVariable Long studentId) {
        List<EvaluationResult> evaluations = evaluationService.getEvaluationsByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", evaluations));
    }

    @PostMapping("/{submissionId}")
    public ResponseEntity<ApiResponse<EvaluationResult>> evaluateSubmission(@PathVariable Long submissionId) {
        EvaluationResult evaluation = evaluationService.evaluateSubmission(submissionId);
        return ResponseEntity.ok(ApiResponse.success("评价成功", evaluation));
    }
}
