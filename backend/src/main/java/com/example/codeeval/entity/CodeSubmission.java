package com.example.codeeval.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 代码提交记录实体类
 */
@Entity
@Table(name = "code_submissions")
public class CodeSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /**
     * 提交时的 Git Commit Hash（可选）。
     * 粘贴代码提交时为 null；GitHub 仓库提交时若能解析到默认分支最新 commit 则填充。
     */
    @Column(length = 64)
    private String gitCommitHash;

    /**
     * 通用仓库 URL（兼容历史 Git 提交语义）。
     * 粘贴代码提交时为 null；GitHub 提交时与 githubUrl 保持一致。
     */
    @Column(length = 512)
    private String repositoryUrl;

    /**
     * 【新增】GitHub 仓库 URL（学生通过"GitHub 提交"粘贴的源链接）。
     * 例如：https://github.com/user/repo 或带 /tree/main 等子路径的形式。
     * 粘贴代码提交时为 null，教师端据此判断是否展示 GitHub 跳转链接。
     */
    @Column(name = "github_url", length = 512)
    private String githubUrl;

    /**
     * 【新增】GitHub 分支名。
     * 学生填写时使用填写值；未填写时默认尝试 main 回退 master。
     * 粘贴代码提交时为 null。
     */
    @Column(name = "github_branch", length = 128)
    private String githubBranch;

    @Column(columnDefinition = "TEXT")
    private String codeContent;

    @Column(nullable = false)
    private Integer submitCount = 1;

    @Column(nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(nullable = false)
    private Boolean isLatest = true;

    public CodeSubmission() {
        this.submitCount = 1;
        this.submittedAt = LocalDateTime.now();
        this.isLatest = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Assignment getAssignment() { return assignment; }
    public void setAssignment(Assignment assignment) { this.assignment = assignment; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public String getGitCommitHash() { return gitCommitHash; }
    public void setGitCommitHash(String gitCommitHash) { this.gitCommitHash = gitCommitHash; }

    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    public String getGithubBranch() { return githubBranch; }
    public void setGithubBranch(String githubBranch) { this.githubBranch = githubBranch; }

    public String getCodeContent() { return codeContent; }
    public void setCodeContent(String codeContent) { this.codeContent = codeContent; }

    public Integer getSubmitCount() { return submitCount; }
    public void setSubmitCount(Integer submitCount) { this.submitCount = submitCount; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public Boolean getIsLatest() { return isLatest; }
    public void setIsLatest(Boolean isLatest) { this.isLatest = isLatest; }

    public static CodeSubmissionBuilder builder() { return new CodeSubmissionBuilder(); }

    public static class CodeSubmissionBuilder {
        private Assignment assignment;
        private User student;
        private String gitCommitHash, repositoryUrl, githubUrl, githubBranch, codeContent;
        private Integer submitCount = 1;
        private LocalDateTime submittedAt = LocalDateTime.now();
        private Boolean isLatest = true;

        public CodeSubmissionBuilder assignment(Assignment a) { this.assignment = a; return this; }
        public CodeSubmissionBuilder student(User s) { this.student = s; return this; }
        public CodeSubmissionBuilder gitCommitHash(String h) { this.gitCommitHash = h; return this; }
        public CodeSubmissionBuilder repositoryUrl(String u) { this.repositoryUrl = u; return this; }
        public CodeSubmissionBuilder githubUrl(String u) { this.githubUrl = u; return this; }
        public CodeSubmissionBuilder githubBranch(String b) { this.githubBranch = b; return this; }
        public CodeSubmissionBuilder codeContent(String c) { this.codeContent = c; return this; }
        public CodeSubmissionBuilder submitCount(Integer n) { this.submitCount = n; return this; }
        public CodeSubmissionBuilder submittedAt(LocalDateTime t) { this.submittedAt = t; return this; }
        public CodeSubmissionBuilder isLatest(Boolean l) { this.isLatest = l; return this; }
        public CodeSubmission build() {
            CodeSubmission s = new CodeSubmission();
            s.setAssignment(assignment); s.setStudent(student); s.setGitCommitHash(gitCommitHash);
            s.setRepositoryUrl(repositoryUrl); s.setGithubUrl(githubUrl); s.setGithubBranch(githubBranch);
            s.setCodeContent(codeContent); s.setSubmitCount(submitCount);
            s.setSubmittedAt(submittedAt); s.setIsLatest(isLatest);
            return s;
        }
    }
}
