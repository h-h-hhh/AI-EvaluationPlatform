package com.example.codeeval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 代码提交请求DTO
 */
public class SubmissionRequest {

    @NotNull(message = "作业ID不能为空")
    private Long assignmentId;

    private String gitCommitHash;

    @NotBlank(message = "仓库URL不能为空")
    private String repositoryUrl;

    private String codeContent;

    public SubmissionRequest() {}

    public SubmissionRequest(Long assignmentId, String gitCommitHash, String repositoryUrl, String codeContent) {
        this.assignmentId = assignmentId;
        this.gitCommitHash = gitCommitHash;
        this.repositoryUrl = repositoryUrl;
        this.codeContent = codeContent;
    }

    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }

    public String getGitCommitHash() { return gitCommitHash; }
    public void setGitCommitHash(String gitCommitHash) { this.gitCommitHash = gitCommitHash; }

    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }

    public String getCodeContent() { return codeContent; }
    public void setCodeContent(String codeContent) { this.codeContent = codeContent; }
}
