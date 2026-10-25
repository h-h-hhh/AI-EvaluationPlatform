package com.example.codeeval.dto;

import com.example.codeeval.entity.Assignment;

/**
 * 教师端作业列表 DTO（解决 assignment submissionCount 显示为 0 的问题）
 *
 * 根因：Assignment 实体没有 submissionCount 字段，前端 Assignments.vue 直接写死为 0。
 * 此处新增轻量 DTO，由 AssignmentService 基于 CodeSubmissionRepository 做聚合后返回。
 * 字段全部平铺，避免 course LAZY 代理问题。
 */
public class AssignmentDTO {

    private Long id;
    private String title;
    private Long courseId;
    private String courseName;
    private Integer totalScore;
    /** deadline ISO 字符串（如 "2026-09-01T12:00:00"），前端自行做 T→空格 展示 */
    private String deadline;
    private Boolean active;
    private String description;
    /** 该作业下的代码提交记录数（来自 CodeSubmissionRepository.countByAssignmentId） */
    private Long submissionCount;

    public static AssignmentDTO from(Assignment a, long submissionCount) {
        AssignmentDTO dto = new AssignmentDTO();
        dto.id = a.getId();
        dto.title = a.getTitle();
        dto.totalScore = a.getTotalScore();
        dto.active = a.getActive();
        dto.description = a.getDescription();
        dto.deadline = a.getDeadline() != null ? a.getDeadline().toString() : null;
        dto.submissionCount = submissionCount;
        if (a.getCourse() != null) {
            dto.courseId = a.getCourse().getId();
            dto.courseName = a.getCourse().getName();
        }
        return dto;
    }

    // Getters / Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getSubmissionCount() { return submissionCount; }
    public void setSubmissionCount(Long submissionCount) { this.submissionCount = submissionCount; }
}
