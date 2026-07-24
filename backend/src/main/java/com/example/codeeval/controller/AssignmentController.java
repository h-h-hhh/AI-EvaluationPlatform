package com.example.codeeval.controller;

import com.example.codeeval.dto.ApiResponse;
import com.example.codeeval.dto.AssignmentRequest;
import com.example.codeeval.entity.Assignment;
import com.example.codeeval.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 作业控制器
 */
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Assignment>>> getAllAssignments() {
        List<Assignment> assignments = assignmentService.getAllActiveAssignments();
        return ResponseEntity.ok(ApiResponse.success("查询成功", assignments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Assignment>> getAssignmentById(@PathVariable Long id) {
        Assignment assignment = assignmentService.getAssignmentById(id);
        return ResponseEntity.ok(ApiResponse.success("查询成功", assignment));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<Assignment>>> getAssignmentsByCourse(@PathVariable Long courseId) {
        List<Assignment> assignments = assignmentService.getAssignmentsByCourse(courseId);
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
