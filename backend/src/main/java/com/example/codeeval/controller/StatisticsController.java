package com.example.codeeval.controller;

import com.example.codeeval.dto.ApiResponse;
import com.example.codeeval.entity.User;
import com.example.codeeval.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AssignmentRepository assignmentRepository;
    private final CodeSubmissionRepository submissionRepository;
    private final EvaluationResultRepository evaluationRepository;

    public StatisticsController(UserRepository userRepository,
                               CourseRepository courseRepository,
                               AssignmentRepository assignmentRepository,
                               CodeSubmissionRepository submissionRepository,
                               EvaluationResultRepository evaluationRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.evaluationRepository = evaluationRepository;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview() {
        Map<String, Object> result = new HashMap<>();

        long totalUsers = userRepository.count();
        long teachers = userRepository.countByRole(User.Role.TEACHER);
        long students = userRepository.countByRole(User.Role.STUDENT);
        long admins = userRepository.countByRole(User.Role.ADMIN);
        long courses = courseRepository.count();
        long assignments = assignmentRepository.count();
        long submissions = submissionRepository.count();
        long evaluations = evaluationRepository.count();

        Double avgScore = evaluationRepository.count() > 0
                ? evaluationRepository.averageScore() : 0.0;

        result.put("totalUsers", totalUsers);
        result.put("teachers", teachers);
        result.put("students", students);
        result.put("admins", admins);
        result.put("courses", courses);
        result.put("assignments", assignments);
        result.put("submissions", submissions);
        result.put("evaluations", evaluations);
        result.put("avgScore", avgScore != null ? Math.round(avgScore) : 0);

        return ResponseEntity.ok(ApiResponse.success("查询成功", result));
    }

    @GetMapping("/user-role-distribution")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserRoleDistribution() {
        Map<String, Object> result = new HashMap<>();

        long admins = userRepository.countByRole(User.Role.ADMIN);
        long teachers = userRepository.countByRole(User.Role.TEACHER);
        long students = userRepository.countByRole(User.Role.STUDENT);
        long total = admins + teachers + students;

        result.put("admin", admins);
        result.put("teacher", teachers);
        result.put("student", students);
        result.put("total", total);

        return ResponseEntity.ok(ApiResponse.success("查询成功", result));
    }

    @GetMapping("/course-submissions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCourseSubmissions() {
        List<Object[]> results = submissionRepository.countByCourse();

        List<Map<String, Object>> data = results.stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("courseName", row[0]);
            map.put("submissions", row[1]);
            return map;
        }).toList();

        return ResponseEntity.ok(ApiResponse.success("查询成功", data));
    }

    @GetMapping("/recent-users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentUsers() {
        List<User> users = userRepository.findAll();
        users.sort((u1, u2) -> {
            if (u1.getId() == null || u2.getId() == null) return 0;
            return Long.compare(u2.getId(), u1.getId());
        });
        users = users.stream().limit(10).toList();

        List<Map<String, Object>> data = users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("name", user.getName());
            map.put("email", user.getEmail());
            map.put("role", user.getRole());
            map.put("createdAt", "");
            map.put("active", user.getEnabled());
            return map;
        }).toList();

        return ResponseEntity.ok(ApiResponse.success("查询成功", data));
    }

    @GetMapping("/course-list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCourseList() {
        List<Map<String, Object>> data = courseRepository.findAll().stream().map(course -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", course.getId());
            map.put("name", course.getName());
            map.put("description", course.getDescription());
            map.put("teacher", course.getTeacher() != null ? course.getTeacher().getName() : "");
            map.put("teacherId", course.getTeacher() != null ? course.getTeacher().getId() : null);
            map.put("active", course.getActive());
            return map;
        }).toList();

        return ResponseEntity.ok(ApiResponse.success("查询成功", data));
    }
}
