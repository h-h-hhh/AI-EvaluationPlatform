package com.example.codeeval.controller;

import com.example.codeeval.dto.ApiResponse;
import com.example.codeeval.dto.AssignmentDTO;
import com.example.codeeval.dto.AssignmentRequest;
import com.example.codeeval.entity.Assignment;
import com.example.codeeval.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 作业控制器
 *
 * 2026-08-31 变更：
 *  - 查询接口（getAllAssignments / getAssignmentsByCourse / getAssignmentById）返回 AssignmentDTO，
 *    其中 submissionCount 由后端基于 code_submission 表聚合，不再让前端 hard-code 0
 *  - create/update 仍返回 Assignment 实体（创建/更新完需回显 id、deadline 等值给表单回填，
 *    且不依赖任何 LAZY 集合，风险低）
 */
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AssignmentDTO>>> getAllAssignments() {
        List<AssignmentDTO> assignments = assignmentService.getAllActiveAssignmentsDTO();
        return ResponseEntity.ok(ApiResponse.success("查询成功", assignments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Assignment>> getAssignmentById(@PathVariable Long id) {
        Assignment assignment = assignmentService.getAssignmentById(id);
        return ResponseEntity.ok(ApiResponse.success("查询成功", assignment));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<AssignmentDTO>>> getAssignmentsByCourse(@PathVariable Long courseId) {
        List<AssignmentDTO> assignments = assignmentService.getAssignmentsByCourseDTO(courseId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", assignments));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Assignment>> createAssignment(@Valid @RequestBody AssignmentRequest request) {
        Assignment assignment = assignmentService.createAssignment(request);
        return ResponseEntity.ok(ApiResponse.success("创建成功", assignment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Assignment>> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRequest request) {
        Assignment assignment = assignmentService.updateAssignment(id, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", assignment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAssignment(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}
