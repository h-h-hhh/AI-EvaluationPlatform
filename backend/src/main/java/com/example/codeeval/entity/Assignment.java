package com.example.codeeval.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 作业实体类
 */
@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private Integer totalScore = 100;

    @Column(columnDefinition = "TEXT")
    private String testCases;

    @Column(columnDefinition = "TEXT")
    private String gradingRules;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    public Assignment() {
        this.totalScore = 100;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }

    public String getTestCases() { return testCases; }
    public void setTestCases(String testCases) { this.testCases = testCases; }

    public String getGradingRules() { return gradingRules; }
    public void setGradingRules(String gradingRules) { this.gradingRules = gradingRules; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static AssignmentBuilder builder() { return new AssignmentBuilder(); }

    public static class AssignmentBuilder {
        private String title, description;
        private Course course;
        private Integer totalScore = 100;
        private String testCases, gradingRules;
        private LocalDateTime deadline;
        private Boolean active = true;
        private LocalDateTime createdAt = LocalDateTime.now();

        public AssignmentBuilder title(String title) { this.title = title; return this; }
        public AssignmentBuilder description(String desc) { this.description = desc; return this; }
        public AssignmentBuilder course(Course c) { this.course = c; return this; }
        public AssignmentBuilder totalScore(Integer s) { this.totalScore = s; return this; }
        public AssignmentBuilder testCases(String tc) { this.testCases = tc; return this; }
        public AssignmentBuilder gradingRules(String gr) { this.gradingRules = gr; return this; }
        public AssignmentBuilder deadline(LocalDateTime d) { this.deadline = d; return this; }
        public AssignmentBuilder status(Boolean a) { this.active = a; return this; }
        public AssignmentBuilder createdAt(LocalDateTime t) { this.createdAt = t; return this; }
        public Assignment build() {
            Assignment a = new Assignment();
            a.setTitle(title); a.setDescription(description); a.setCourse(course);
            a.setTotalScore(totalScore); a.setTestCases(testCases); a.setGradingRules(gradingRules);
            a.setDeadline(deadline); a.setActive(active); a.setCreatedAt(createdAt);
            return a;
        }
    }
}
