package com.example.codeeval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 作业请求DTO
 */
public class AssignmentRequest {

    @NotBlank(message = "作业标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    private Integer totalScore = 100;

    private String testCases;

    private String gradingRules;

    @NotNull(message = "截止日期不能为空")
    private LocalDateTime deadline;

    public AssignmentRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }

    public String getTestCases() { return testCases; }
    public void setTestCases(String testCases) { this.testCases = testCases; }

    public String getGradingRules() { return gradingRules; }
    public void setGradingRules(String gradingRules) { this.gradingRules = gradingRules; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
}
