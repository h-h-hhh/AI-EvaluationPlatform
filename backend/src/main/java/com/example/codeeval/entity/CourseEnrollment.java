package com.example.codeeval.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 课程-学生关联实体类
 */
@Entity
@Table(name = "course_enrollments")
public class CourseEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private LocalDateTime enrolledAt = LocalDateTime.now();

    public CourseEnrollment() {
        this.active = true;
        this.enrolledAt = LocalDateTime.now();
    }

    public CourseEnrollment(Course course, User student) {
        this.course = course;
        this.student = student;
        this.active = true;
        this.enrolledAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }

    public static CourseEnrollmentBuilder builder() { return new CourseEnrollmentBuilder(); }

    public static class CourseEnrollmentBuilder {
        private Course course;
        private User student;
        private Boolean active = true;
        private LocalDateTime enrolledAt = LocalDateTime.now();

        public CourseEnrollmentBuilder course(Course c) { this.course = c; return this; }
        public CourseEnrollmentBuilder student(User s) { this.student = s; return this; }
        public CourseEnrollmentBuilder status(Boolean a) { this.active = a; return this; }
        public CourseEnrollmentBuilder enrolledAt(LocalDateTime t) { this.enrolledAt = t; return this; }
        public CourseEnrollment build() {
            CourseEnrollment e = new CourseEnrollment();
            e.setCourse(course); e.setStudent(student); e.setActive(active); e.setEnrolledAt(enrolledAt);
            return e;
        }
    }
}
