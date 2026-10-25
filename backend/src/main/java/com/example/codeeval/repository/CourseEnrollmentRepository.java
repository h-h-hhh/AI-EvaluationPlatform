package com.example.codeeval.repository;

import com.example.codeeval.entity.Course;
import com.example.codeeval.entity.CourseEnrollment;
import com.example.codeeval.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 课程-学生关联Repository接口
 */
@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    List<CourseEnrollment> findByCourse(Course course);

    List<CourseEnrollment> findByStudent(User student);

    List<CourseEnrollment> findByCourseId(Long courseId);

    List<CourseEnrollment> findByStudentId(Long studentId);

    Optional<CourseEnrollment> findByCourseAndStudent(Course course, User student);

    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);

    Optional<CourseEnrollment> findByCourseIdAndStudentId(Long courseId, Long studentId);

    long countByCourseId(Long courseId);
    
    void deleteByCourseId(Long courseId);
}
