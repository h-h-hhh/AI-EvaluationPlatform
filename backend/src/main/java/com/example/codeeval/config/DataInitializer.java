package com.example.codeeval.config;

import com.example.codeeval.entity.*;
import com.example.codeeval.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(
            UserRepository userRepository,
            CourseRepository courseRepository,
            CourseEnrollmentRepository enrollmentRepository,
            AssignmentRepository assignmentRepository,
            PasswordEncoder passwordEncoder) {
        
        return args -> {
            if (userRepository.count() == 0) {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("password"))
                        .email("admin@codeeval.com")
                        .name("管理员")
                        .role(User.Role.ADMIN)
                        .enabled(true)
                        .build();
                userRepository.save(admin);

                User teacher = User.builder()
                        .username("teacher")
                        .password(passwordEncoder.encode("password"))
                        .email("teacher@codeeval.com")
                        .name("张老师")
                        .role(User.Role.TEACHER)
                        .enabled(true)
                        .build();
                userRepository.save(teacher);

                User student = User.builder()
                        .username("student")
                        .password(passwordEncoder.encode("password"))
                        .email("student@codeeval.com")
                        .name("学生小王")
                        .role(User.Role.STUDENT)
                        .enabled(true)
                        .build();
                userRepository.save(student);

                Course course1 = Course.builder()
                        .name("Java程序设计")
                        .description("Java基础语法、面向对象编程、集合框架等")
                        .code("CS101")
                        .teacher(teacher)
                        .status(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                courseRepository.save(course1);

                Course course2 = Course.builder()
                        .name("数据结构与算法")
                        .description("线性表、树、图、排序、查找等")
                        .code("CS102")
                        .teacher(teacher)
                        .status(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                courseRepository.save(course2);

                CourseEnrollment enrollment1 = CourseEnrollment.builder()
                        .course(course1)
                        .student(student)
                        .status(true)
                        .enrolledAt(LocalDateTime.now())
                        .build();
                enrollmentRepository.save(enrollment1);

                CourseEnrollment enrollment2 = CourseEnrollment.builder()
                        .course(course2)
                        .student(student)
                        .status(true)
                        .enrolledAt(LocalDateTime.now())
                        .build();
                enrollmentRepository.save(enrollment2);

                Assignment assignment1 = Assignment.builder()
                        .title("Java基础练习 - 数组排序")
                        .description("实现一个数组排序算法，可以选择冒泡排序、快速排序等")
                        .course(course1)
                        .deadline(LocalDateTime.now().plusDays(30))
                        .totalScore(100)
                        .status(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                assignmentRepository.save(assignment1);

                Assignment assignment2 = Assignment.builder()
                        .title("数据结构 - 链表实现")
                        .description("实现单链表的基本操作：增删改查")
                        .course(course2)
                        .deadline(LocalDateTime.now().plusDays(30))
                        .totalScore(100)
                        .status(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                assignmentRepository.save(assignment2);

                System.out.println("初始化数据完成");
            }
        };
    }
}