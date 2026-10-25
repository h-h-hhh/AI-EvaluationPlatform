package com.example.codeeval.service;

import com.example.codeeval.dto.AssignmentDTO;
import com.example.codeeval.dto.AssignmentRequest;
import com.example.codeeval.entity.Assignment;
import com.example.codeeval.entity.Course;
import com.example.codeeval.repository.AssignmentRepository;
import com.example.codeeval.repository.CodeSubmissionRepository;
import com.example.codeeval.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作业服务类
 *
 * 2026-08-31 变更：查询接口返回 AssignmentDTO（而非 Assignment 实体），
 * 其中 submissionCount 由 CodeSubmissionRepository.countByAssignmentId 聚合得到，
 * 解决教师端作业列表"提交人数永远是 0"的问题。
 */
@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final CodeSubmissionRepository codeSubmissionRepository;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             CourseRepository courseRepository,
                             CodeSubmissionRepository codeSubmissionRepository) {
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
        this.codeSubmissionRepository = codeSubmissionRepository;
    }

    public Assignment createAssignment(AssignmentRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("课程不存在"));

        Assignment assignment = Assignment.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .course(course)
                .totalScore(request.getTotalScore() != null ? request.getTotalScore() : 100)
                .testCases(request.getTestCases())
                .gradingRules(request.getGradingRules())
                .deadline(request.getDeadline())
                .status(true)
                .createdAt(LocalDateTime.now())
                .build();

        return assignmentRepository.save(assignment);
    }

    public Assignment updateAssignment(Long id, AssignmentRequest request) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("作业不存在"));

        if (request.getCourseId() != null) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new IllegalArgumentException("课程不存在"));
            assignment.setCourse(course);
        }

        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        if (request.getTotalScore() != null) {
            assignment.setTotalScore(request.getTotalScore());
        }
        assignment.setTestCases(request.getTestCases());
        assignment.setGradingRules(request.getGradingRules());
        assignment.setDeadline(request.getDeadline());
        assignment.setUpdatedAt(LocalDateTime.now());

        return assignmentRepository.save(assignment);
    }

    public void deleteAssignment(Long id) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("作业不存在"));
        assignment.setActive(false);
        assignment.setUpdatedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
    }

    public Assignment getAssignmentById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("作业不存在"));
    }

    // ==================== 查询方法：返回 DTO，附带 submissionCount 聚合 ====================
    // getAll/getByCourse 会访问 course LAZY 关联，开只读事务保证代理安全初始化

    @Transactional(readOnly = true)
    public List<AssignmentDTO> getAssignmentsByCourseDTO(Long courseId) {
        return assignmentRepository.findByCourseId(courseId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssignmentDTO> getAllActiveAssignmentsDTO() {
        return assignmentRepository.findByActiveTrue().stream()
                .map(this::toDTO)
                .toList();
    }

    private AssignmentDTO toDTO(Assignment a) {
        long count = codeSubmissionRepository.countByAssignmentId(a.getId());
        return AssignmentDTO.from(a, count);
    }
}
