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

    @Column(nullable = false)
    private String gitCommitHash;

    @Column(nullable = false)
    private String repositoryUrl;

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
        private String gitCommitHash, repositoryUrl, codeContent;
        private Integer submitCount = 1;
        private LocalDateTime submittedAt = LocalDateTime.now();
        private Boolean isLatest = true;

        public CodeSubmissionBuilder assignment(Assignment a) { this.assignment = a; return this; }
        public CodeSubmissionBuilder student(User s) { this.student = s; return this; }
        public CodeSubmissionBuilder gitCommitHash(String h) { this.gitCommitHash = h; return this; }
        public CodeSubmissionBuilder repositoryUrl(String u) { this.repositoryUrl = u; return this; }
        public CodeSubmissionBuilder codeContent(String c) { this.codeContent = c; return this; }
        public CodeSubmissionBuilder submitCount(Integer n) { this.submitCount = n; return this; }
        public CodeSubmissionBuilder submittedAt(LocalDateTime t) { this.submittedAt = t; return this; }
        public CodeSubmissionBuilder isLatest(Boolean l) { this.isLatest = l; return this; }
        public CodeSubmission build() {
            CodeSubmission s = new CodeSubmission();
            s.setAssignment(assignment); s.setStudent(student); s.setGitCommitHash(gitCommitHash);
            s.setRepositoryUrl(repositoryUrl); s.setCodeContent(codeContent); s.setSubmitCount(submitCount);
            s.setSubmittedAt(submittedAt); s.setIsLatest(isLatest);
            return s;
        }
    }
}
