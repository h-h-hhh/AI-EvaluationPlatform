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

    /**
     * 通用提交入口（兼容历史 & 两种新模式）。
     *
     * 模式判定：
     *  - 当 request.githubUrl 非空白 → GitHub 仓库提交：由调用方在 Controller 层先调用 GitService 下载
     *    ZIP 并填充 codeContent / githubUrl / githubBranch；本方法只负责持久化。
     *  - 否则当 request.codeContent 非空白 → 粘贴代码提交。
     *  - 否则当 request.repositoryUrl 非空白（历史老前端 Git 提交）→ 保持原样。
     *  - 都不满足 → 抛 IllegalArgumentException，GlobalExceptionHandler 返回 400 友好错误。
     */
    public CodeSubmission submitCode(SubmissionRequest request, Long studentId) {
        // 基础校验：作业存在、学生存在
        Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException("作业不存在"));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));

        // 模式判定 & 必填字段校验
        boolean isGithub = request.getGithubUrl() != null && !request.getGithubUrl().isBlank();
        boolean hasCode = request.getCodeContent() != null && !request.getCodeContent().isBlank();
        boolean hasOldRepo = request.getRepositoryUrl() != null && !request.getRepositoryUrl().isBlank();

        if (!isGithub && !hasCode && !hasOldRepo) {
            throw new IllegalArgumentException("提交内容不能为空：请提供 GitHub 仓库 URL");
        }
        if (isGithub && !hasCode) {
            // GitHub 模式下 codeContent 必须已经由 Controller 通过 GitService 下载并填充
            throw new IllegalArgumentException("GitHub 仓库下载失败：未能获取到源代码内容，请检查仓库 URL 后重试");
        }

        // 历史版本：把同一学生同一作业的上次 isLatest=true 标记为 false（保留提交历史，仅最新用于评价）
        Optional<CodeSubmission> existingSubmission = submissionRepository
                .findByAssignmentAndStudentAndIsLatestTrue(assignment, student);

        if (existingSubmission.isPresent()) {
            CodeSubmission oldSubmission = existingSubmission.get();
            oldSubmission.setIsLatest(false);
            submissionRepository.save(oldSubmission);
        }

        int submitCount = (int) submissionRepository.countByAssignmentIdAndStudentId(request.getAssignmentId(), studentId) + 1;

        // repositoryUrl 兼容字段：GitHub 模式镜像 githubUrl；其它模式沿用请求中的老字段
        String normalizedRepoUrl = isGithub ? request.getGithubUrl() : request.getRepositoryUrl();

        CodeSubmission submission = CodeSubmission.builder()
                .assignment(assignment)
                .student(student)
                .gitCommitHash(request.getGitCommitHash())
                .repositoryUrl(normalizedRepoUrl)
                .githubUrl(isGithub ? request.getGithubUrl() : null)
                .githubBranch(isGithub ? request.getGithubBranch() : null)
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
