package com.example.codeeval.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 课程实体类
 */
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String code;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    public Course() {
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public Course(String name, String description, String code, User teacher) {
        this.name = name;
        this.description = description;
        this.code = code;
        this.teacher = teacher;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static CourseBuilder builder() { return new CourseBuilder(); }

    public static class CourseBuilder {
        private String name, description, code;
        private User teacher;
        private Boolean active = true;
        private LocalDateTime createdAt = LocalDateTime.now();

        public CourseBuilder name(String name) { this.name = name; return this; }
        public CourseBuilder description(String description) { this.description = description; return this; }
        public CourseBuilder code(String code) { this.code = code; return this; }
        public CourseBuilder teacher(User teacher) { this.teacher = teacher; return this; }
        public CourseBuilder status(Boolean active) { this.active = active; return this; }
        public CourseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Course build() {
            Course c = new Course(name, description, code, teacher);
            c.setActive(active);
            c.setCreatedAt(createdAt);
            return c;
        }
    }
}
