package com.example.codeeval.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 课程请求DTO
 */
public class CourseRequest {

    @NotBlank(message = "课程名称不能为空")
    private String name;

    private String description;

    @NotBlank(message = "课程代码不能为空")
    private String code;

    private Long teacherId;

    private Boolean active;

    public CourseRequest() {}

    public CourseRequest(String name, String description, String code) {
        this.name = name;
        this.description = description;
        this.code = code;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
