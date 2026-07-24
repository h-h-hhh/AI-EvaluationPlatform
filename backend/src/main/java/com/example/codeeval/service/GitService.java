package com.example.codeeval.service;

import com.example.codeeval.entity.*;
import com.example.codeeval.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GitService {

    @Autowired
    private CodeSubmissionRepository codeSubmissionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    /**
     * 获取学生某次作业的Git提交历史
     */
    public Map<String, Object> getStudentSubmissionHistory(Long studentId, Long assignmentId) {
        Map<String, Object> result = new HashMap<>();
        
        List<CodeSubmission> submissions = codeSubmissionRepository.findByStudentId(studentId);
        submissions = submissions.stream()
            .filter(s -> s.getAssignment() != null && s.getAssignment().getId().equals(assignmentId))
            .collect(Collectors.toList());
        
        result.put("commitCount", submissions.size());
        result.put("submissions", submissions.stream().map(this::convertToDto).collect(Collectors.toList()));
        
        // 计算提交频率
        if (submissions.size() > 1) {
            submissions.sort(Comparator.comparing(CodeSubmission::getSubmittedAt));
            LocalDateTime first = submissions.get(0).getSubmittedAt();
            LocalDateTime last = submissions.get(submissions.size() - 1).getSubmittedAt();
            long days = java.time.temporal.ChronoUnit.DAYS.between(first, last);
            double frequency = days > 0 ? (double) submissions.size() / days : submissions.size();
            result.put("commitFrequency", frequency > 2 ? "高" : frequency > 0.5 ? "中" : "低");
        } else {
            result.put("commitFrequency", submissions.isEmpty() ? "无" : "低");
        }
        
        return result;
    }

    /**
     * 获取仓库分析信息
     */
    public Map<String, Object> getRepositoryAnalysis(Long assignmentId) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<Assignment> assignmentOpt = assignmentRepository.findById(assignmentId);
        if (assignmentOpt.isEmpty()) {
            return result;
        }
        
        Assignment assignment = assignmentOpt.get();
        // 获取作业的基本信息
        result.put("title", assignment.getTitle());
        result.put("description", assignment.getDescription());
        result.put("totalScore", assignment.getTotalScore());
        result.put("deadline", assignment.getDeadline());
        
        // 统计提交情况
        List<CodeSubmission> submissions = codeSubmissionRepository.findByAssignmentId(assignmentId);
        result.put("totalSubmissions", submissions.size());
        result.put("submittedStudents", submissions.stream()
            .map(CodeSubmission::getStudent)
            .filter(Objects::nonNull)
            .map(User::getId)
            .collect(Collectors.toSet()).size());
        
        return result;
    }

    /**
     * 验证Git仓库URL格式
     */
    public Map<String, Object> validateGitUrl(String repoUrl, String repoType) {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", false);
        result.put("message", "");
        
        if (repoUrl == null || repoUrl.trim().isEmpty()) {
            result.put("message", "仓库URL不能为空");
            return result;
        }
        
        // 简单的URL格式验证
        String httpsPrefix = "";
        switch (repoType) {
            case "github":
                httpsPrefix = "https://github.com/";
                break;
            case "gitlab":
                httpsPrefix = "https://gitlab.com/";
                break;
            case "gitee":
                httpsPrefix = "https://gitee.com/";
                break;
            default:
                // 自定义仓库不验证前缀
                result.put("valid", true);
                return result;
        }
        
        if (repoUrl.startsWith(httpsPrefix)) {
            result.put("valid", true);
        } else if (repoUrl.startsWith("http://") || repoUrl.startsWith("https://")) {
            result.put("message", "URL格式不正确，应为 " + httpsPrefix + "xxx/repo");
        } else {
            result.put("message", "URL必须以 " + httpsPrefix + " 开头");
        }
        
        return result;
    }

    private Map<String, Object> convertToDto(CodeSubmission submission) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", submission.getId());
        dto.put("commitHash", submission.getGitCommitHash());
        dto.put("submitTime", submission.getSubmittedAt());
        dto.put("repoUrl", submission.getRepositoryUrl());
        dto.put("submitCount", submission.getSubmitCount());
        dto.put("isLatest", submission.getIsLatest());
        return dto;
    }
}
