package com.example.codeeval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 基于LLM+Git的代码作业智慧评价平台
 * 主应用类
 */
@SpringBootApplication
public class CodeEvalApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeEvalApplication.class, args);
    }
}
