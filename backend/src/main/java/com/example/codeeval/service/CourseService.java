package com.example.codeeval.service;

import com.example.codeeval.dto.CourseRequest;
import com.example.codeeval.entity.Course;
import com.example.codeeval.entity.CourseEnrollment;
import com.example.codeeval.entity.User;
import com.example.codeeval.repository.AssignmentRepository;
import com.example.codeeval.repository.CodeSubmissionRepository;
import com.example.codeeval.repository.CourseEnrollmentRepository;
import com.example.codeeval.repository.CourseRepository;
import com.example.codeeval.repository.EvaluationResultRepository;
import com.example.codeeval.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 课程服务类
 */
@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final CodeSubmissionRepository submissionRepository;
    private final EvaluationResultRepository evaluationResultRepository;

    public CourseService(CourseRepository courseRepository, UserRepository userRepository,
                        CourseEnrollmentRepository enrollmentRepository,
                        AssignmentRepository assignmentRepository,
                        CodeSubmissionRepository submissionRepository,
                        EvaluationResultRepository evaluationResultRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.evaluationResultRepository = evaluationResultRepository;
    }

    public Course createCourse(CourseRequest request, Long currentUserId) {
        Long teacherId = request.getTeacherId() != null ? request.getTeacherId() : currentUserId;
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("教师不存在"));

        if (courseRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("课程代码已存在");
        }

        Course course = Course.builder()
                .name(request.getName())
                .description(request.getDescription())
                .code(request.getCode())
                .teacher(teacher)
                .status(request.getActive() != null ? request.getActive() : true)
                .createdAt(LocalDateTime.now())
                .build();

        return courseRepository.save(course);
    }

    public Course updateCourse(Long id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("课程不存在"));

        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course.setCode(request.getCode());
        course.setUpdatedAt(LocalDateTime.now());

        return courseRepository.save(course);
    }

    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("课程不存在"));
        
        assignmentRepository.findByCourseId(id).forEach(assignment -> {
            submissionRepository.findByAssignmentId(assignment.getId()).forEach(submission -> {
                evaluationResultRepository.deleteBySubmissionId(submission.getId());
            });
            submissionRepository.deleteByAssignmentId(assignment.getId());
        });
        
        assignmentRepository.deleteByCourseId(id);
        enrollmentRepository.deleteByCourseId(id);
        
        courseRepository.delete(course);
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("课程不存在"));
    }

    public List<Course> getCoursesByTeacher(Long teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("教师不存在"));
        return courseRepository.findByTeacher(teacher);
    }

    public List<Course> getAllActiveCourses() {
        return courseRepository.findByActiveTrue();
    }

    public List<Course> getEnrolledCourses(Long studentId) {
        List<CourseEnrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        return enrollments.stream()
                // 只取有效（未退课）的选课记录，避免退课后仍出现在已选列表
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .map(CourseEnrollment::getCourse)
                .filter(Course::getActive)
                .collect(Collectors.toList());
    }

    public CourseEnrollment enrollCourse(Long studentId, Long courseId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("课程不存在"));
        
        if (!course.getActive()) {
            throw new IllegalArgumentException("课程已关闭");
        }
        
        if (enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new IllegalArgumentException("已选该课程");
        }
        
        CourseEnrollment enrollment = CourseEnrollment.builder()
                .course(course)
                .student(student)
                .status(true)
                .enrolledAt(LocalDateTime.now())
                .build();
        
        return enrollmentRepository.save(enrollment);
    }

    public void dropCourse(Long studentId, Long courseId) {
        CourseEnrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未选择该课程"));
        
        enrollment.setActive(false);
        enrollmentRepository.save(enrollment);
    }

    public List<Course> getAvailableCourses(Long studentId) {
        List<Course> allCourses = courseRepository.findByActiveTrue();
        // 仅统计 active=true 的选课记录；退课后（active=false）的课程需要重新出现在可选列表中
        List<Long> enrolledCourseIds = enrollmentRepository.findByStudentId(studentId)
                .stream()
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .map(e -> e.getCourse().getId())
                .collect(Collectors.toList());
        
        return allCourses.stream()
                .filter(c -> !enrolledCourseIds.contains(c.getId()))
                .collect(Collectors.toList());
    }
}
