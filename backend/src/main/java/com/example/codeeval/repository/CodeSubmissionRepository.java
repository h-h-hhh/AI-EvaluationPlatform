package com.example.codeeval.repository;

import com.example.codeeval.entity.Assignment;
import com.example.codeeval.entity.CodeSubmission;
import com.example.codeeval.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 代码提交记录Repository接口
 */
@Repository
public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, Long> {

    List<CodeSubmission> findByAssignment(Assignment assignment);

    List<CodeSubmission> findByStudent(User student);

    List<CodeSubmission> findByAssignmentId(Long assignmentId);

    List<CodeSubmission> findByStudentId(Long studentId);

    Optional<CodeSubmission> findByAssignmentAndStudentAndIsLatestTrue(Assignment assignment, User student);

    long countByAssignmentId(Long assignmentId);

    long countByStudentId(Long studentId);

    long countByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    @org.springframework.data.jpa.repository.Query("SELECT a.course.name, COUNT(s) FROM CodeSubmission s JOIN s.assignment a GROUP BY a.course.name")
    List<Object[]> countByCourse();
    
    void deleteByAssignmentId(Long assignmentId);
}
