package com.example.codeeval.repository;

import com.example.codeeval.entity.Course;
import com.example.codeeval.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 课程Repository接口
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByTeacher(User teacher);

    List<Course> findByActiveTrue();

    boolean existsByCode(String code);

    Optional<Course> findByCode(String code);
}
