package com.example.codeeval.controller;

import com.example.codeeval.dto.ApiResponse;
import com.example.codeeval.dto.SubmissionRequest;
import com.example.codeeval.entity.CodeSubmission;
import com.example.codeeval.entity.User;
import com.example.codeeval.service.CodeSubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代码提交控制器
 */
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final CodeSubmissionService submissionService;

    public SubmissionController(CodeSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CodeSubmission>>> getAllSubmissions() {
        throw new UnsupportedOperationException("请使用具体的查询接口");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CodeSubmission>> getSubmissionById(@PathVariable Long id) {
        CodeSubmission submission = submissionService.getSubmissionById(id);
        return ResponseEntity.ok(ApiResponse.success("查询成功", submission));
    }

    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<ApiResponse<List<CodeSubmission>>> getSubmissionsByAssignment(@PathVariable Long assignmentId) {
        List<CodeSubmission> submissions = submissionService.getSubmissionsByAssignment(assignmentId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", submissions));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<CodeSubmission>>> getSubmissionsByStudent(@PathVariable Long studentId) {
        List<CodeSubmission> submissions = submissionService.getSubmissionsByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", submissions));
    }

    @GetMapping("/latest/{assignmentId}")
    public ResponseEntity<ApiResponse<CodeSubmission>> getLatestSubmission(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal User user) {
        CodeSubmission submission = submissionService.getLatestSubmission(assignmentId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("查询成功", submission));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CodeSubmission>> submitCode(
            @Valid @RequestBody SubmissionRequest request,
            @AuthenticationPrincipal User user) {
        CodeSubmission submission = submissionService.submitCode(request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("提交成功", submission));
    }
}
