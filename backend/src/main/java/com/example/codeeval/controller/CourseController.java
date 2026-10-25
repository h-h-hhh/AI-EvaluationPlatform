package com.example.codeeval.controller;

import com.example.codeeval.dto.ApiResponse;
import com.example.codeeval.dto.CourseRequest;
import com.example.codeeval.entity.Course;
import com.example.codeeval.entity.User;
import com.example.codeeval.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程控制器
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Course>>> getAllCourses() {
        List<Course> courses = courseService.getAllActiveCourses();
        return ResponseEntity.ok(ApiResponse.success("查询成功", courses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Course>> getCourseById(@PathVariable Long id) {
        Course course = courseService.getCourseById(id);
        return ResponseEntity.ok(ApiResponse.success("查询成功", course));
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<ApiResponse<List<Course>>> getCoursesByTeacher(@PathVariable Long teacherId) {
        List<Course> courses = courseService.getCoursesByTeacher(teacherId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", courses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Course>> createCourse(
            @Valid @RequestBody CourseRequest request,
            @AuthenticationPrincipal User user) {
        Course course = courseService.createCourse(request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("创建成功", course));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Course>> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequest request) {
        Course course = courseService.updateCourse(id, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", course));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @GetMapping("/student/enrolled")
    public ResponseEntity<ApiResponse<List<Course>>> getEnrolledCourses(@AuthenticationPrincipal User user) {
        List<Course> courses = courseService.getEnrolledCourses(user.getId());
        return ResponseEntity.ok(ApiResponse.success("查询成功", courses));
    }

    @GetMapping("/student/available")
    public ResponseEntity<ApiResponse<List<Course>>> getAvailableCourses(@AuthenticationPrincipal User user) {
        List<Course> courses = courseService.getAvailableCourses(user.getId());
        return ResponseEntity.ok(ApiResponse.success("查询成功", courses));
    }

    @PostMapping("/student/enroll/{courseId}")
    public ResponseEntity<ApiResponse<Course>> enrollCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User user) {
        courseService.enrollCourse(user.getId(), courseId);
        Course course = courseService.getCourseById(courseId);
        return ResponseEntity.ok(ApiResponse.success("选课成功", course));
    }

    @PostMapping("/student/drop/{courseId}")
    public ResponseEntity<ApiResponse<Void>> dropCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User user) {
        courseService.dropCourse(user.getId(), courseId);
        return ResponseEntity.ok(ApiResponse.success("退课成功", null));
    }
}
