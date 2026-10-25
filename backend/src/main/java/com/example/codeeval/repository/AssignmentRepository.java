package com.example.codeeval.repository;

import com.example.codeeval.entity.Assignment;
import com.example.codeeval.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 作业Repository接口
 */
@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByCourse(Course course);

    List<Assignment> findByCourseId(Long courseId);

    List<Assignment> findByActiveTrue();
    
    void deleteByCourseId(Long courseId);
}
