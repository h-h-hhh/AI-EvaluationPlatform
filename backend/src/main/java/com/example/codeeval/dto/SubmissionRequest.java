package com.example.codeeval.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 代码提交请求 DTO（支持"粘贴代码"与"GitHub 仓库 URL"两种提交模式）
 *
 * 两种模式的字段约定：
 *  - 粘贴代码（PASTE）：assignmentId 非空 + codeContent 非空白；githubUrl / githubBranch 应为 null
 *  - GitHub 仓库（GITHUB）：assignmentId 非空 + githubUrl 非空白；githubBranch 可选（默认 main 回退 master）
 *
 * 为兼容老前端提交（已有前端仅填 repositoryUrl 或 codeContent），
 * 这里不使用 Bean Validation 强制字段组合，
 * 由 CodeSubmissionService 在业务层判定模式并抛出 IllegalArgumentException，
 * 再由 GlobalExceptionHandler 包装为 ApiResponse.fail 友好返回。
 */
public class SubmissionRequest {

    @NotNull(message = "作业ID不能为空")
    private Long assignmentId;

    /** 【兼容历史】Git Commit Hash（可空，GitHub 模式时由后端从 ZIP 响应 /commits API 解析后写入） */
    private String gitCommitHash;

    /** 【兼容历史】通用仓库 URL（可空，后端在 GitHub 模式下会把 githubUrl 镜像写入该字段） */
    private String repositoryUrl;

    /** 源代码内容：由后端从 GitHub 仓库下载合并后回填（学生无需填写） */
    private String codeContent;

    /** 【GitHub 模式必填】学生粘贴的公开仓库 URL，如 https://github.com/user/repo 或带 /tree/main 的路径 */
    private String githubUrl;

    /** 【GitHub 模式可选】指定分支；未填时后端默认尝试 main 再回退 master */
    private String githubBranch;

    public SubmissionRequest() {}

    public SubmissionRequest(Long assignmentId, String gitCommitHash, String repositoryUrl,
                             String codeContent, String githubUrl, String githubBranch) {
        this.assignmentId = assignmentId;
        this.gitCommitHash = gitCommitHash;
        this.repositoryUrl = repositoryUrl;
        this.codeContent = codeContent;
        this.githubUrl = githubUrl;
        this.githubBranch = githubBranch;
    }

    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }

    public String getGitCommitHash() { return gitCommitHash; }
    public void setGitCommitHash(String gitCommitHash) { this.gitCommitHash = gitCommitHash; }

    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }

    public String getCodeContent() { return codeContent; }
    public void setCodeContent(String codeContent) { this.codeContent = codeContent; }

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    public String getGithubBranch() { return githubBranch; }
    public void setGithubBranch(String githubBranch) { this.githubBranch = githubBranch; }
}
