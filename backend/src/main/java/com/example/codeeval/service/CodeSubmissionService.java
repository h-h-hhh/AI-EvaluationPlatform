package com.example.codeeval.service;

import com.example.codeeval.dto.SubmissionRequest;
import com.example.codeeval.entity.Assignment;
import com.example.codeeval.entity.CodeSubmission;
import com.example.codeeval.entity.User;
import com.example.codeeval.repository.AssignmentRepository;
import com.example.codeeval.repository.CodeSubmissionRepository;
import com.example.codeeval.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 代码提交服务类
 */
@Service
public class CodeSubmissionService {

    private final CodeSubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    public CodeSubmissionService(CodeSubmissionRepository submissionRepository,
                               AssignmentRepository assignmentRepository,
                               UserRepository userRepository) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
    }

    public CodeSubmission submitCode(SubmissionRequest request, Long studentId) {
        Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException("作业不存在"));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));

        Optional<CodeSubmission> existingSubmission = submissionRepository
                .findByAssignmentAndStudentAndIsLatestTrue(assignment, student);

        if (existingSubmission.isPresent()) {
            CodeSubmission oldSubmission = existingSubmission.get();
            oldSubmission.setIsLatest(false);
            submissionRepository.save(oldSubmission);
        }

        int submitCount = (int) submissionRepository.countByAssignmentIdAndStudentId(request.getAssignmentId(), studentId) + 1;

        CodeSubmission submission = CodeSubmission.builder()
                .assignment(assignment)
                .student(student)
                .gitCommitHash(request.getGitCommitHash())
                .repositoryUrl(request.getRepositoryUrl())
                .codeContent(request.getCodeContent())
                .submitCount(submitCount)
                .submittedAt(LocalDateTime.now())
                .isLatest(true)
                .build();

        return submissionRepository.save(submission);
    }

    public CodeSubmission getSubmissionById(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("提交记录不存在"));
    }

    public List<CodeSubmission> getSubmissionsByAssignment(Long assignmentId) {
        return submissionRepository.findByAssignmentId(assignmentId);
    }

    public List<CodeSubmission> getSubmissionsByStudent(Long studentId) {
        return submissionRepository.findByStudentId(studentId);
    }

    public CodeSubmission getLatestSubmission(Long assignmentId, Long studentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("作业不存在"));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));

        return submissionRepository.findByAssignmentAndStudentAndIsLatestTrue(assignment, student)
                .orElseThrow(() -> new IllegalArgumentException("没有找到提交记录"));
    }
}
