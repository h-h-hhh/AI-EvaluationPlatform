package com.example.codeeval.service;

import com.example.codeeval.dto.AssignmentRequest;
import com.example.codeeval.entity.Assignment;
import com.example.codeeval.entity.Course;
import com.example.codeeval.repository.AssignmentRepository;
import com.example.codeeval.repository.CourseRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作业服务类
 */
@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;

    public AssignmentService(AssignmentRepository assignmentRepository, CourseRepository courseRepository) {
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
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

    public List<Assignment> getAssignmentsByCourse(Long courseId) {
        return assignmentRepository.findByCourseId(courseId);
    }

    public List<Assignment> getAllActiveAssignments() {
        return assignmentRepository.findByActiveTrue();
    }
}
