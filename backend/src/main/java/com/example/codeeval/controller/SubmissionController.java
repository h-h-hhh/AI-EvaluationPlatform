package com.example.codeeval.controller;

import com.example.codeeval.dto.ApiResponse;
import com.example.codeeval.dto.EvaluationStatusDTO;
import com.example.codeeval.dto.SubmissionRequest;
import com.example.codeeval.entity.CodeSubmission;
import com.example.codeeval.entity.EvaluationResult;
import com.example.codeeval.entity.User;
import com.example.codeeval.service.CodeSubmissionService;
import com.example.codeeval.service.EvaluationService;
import com.example.codeeval.service.GitService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码提交控制器
 *
 * 提交方式（唯一）：
 *
 *  POST /api/submissions/github ：GitHub 公开仓库 URL（后端调用 GitService 下载 ZIP →
 *                                 解压 → 合并代码 → 保存 CodeSubmission）
 *
 * 历史上的"粘贴代码"端点 POST /api/submissions 已下线（学生端统一通过 GitHub 公开仓库提交）；
 * 历史粘贴提交记录仍可通过查询接口与评价结果正常查看。
 *
 * 提交成功后立即投递评价任务（异步，HTTP 202），学生端通过 useEvaluationPolling 轮询
 * GET /api/evaluations/status/{submissionId} 获得实时评价进度。
 */
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final CodeSubmissionService submissionService;
    private final EvaluationService evaluationService;
    private final GitService gitService;

    public SubmissionController(CodeSubmissionService submissionService,
                                EvaluationService evaluationService,
                                GitService gitService) {
        this.submissionService = submissionService;
        this.evaluationService = evaluationService;
        this.gitService = gitService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CodeSubmission>>> getAllSubmissions() {
        throw new UnsupportedOperationException("请使用具体的查询接口");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CodeSubmission>> getSubmissionById(@PathVariable Long id) {
        CodeSubmission submission = submissionService.getSubmissionById(id);
        return ResponseEntity.ok(ApiResponse.success("查询成功", submission));
    }

    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<ApiResponse<List<CodeSubmission>>> getSubmissionsByAssignment(@PathVariable Long assignmentId) {
        List<CodeSubmission> submissions = submissionService.getSubmissionsByAssignment(assignmentId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", submissions));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<CodeSubmission>>> getSubmissionsByStudent(@PathVariable Long studentId) {
        List<CodeSubmission> submissions = submissionService.getSubmissionsByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", submissions));
    }

    @GetMapping("/latest/{assignmentId}")
    public ResponseEntity<ApiResponse<CodeSubmission>> getLatestSubmission(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal User user) {
        CodeSubmission submission = submissionService.getLatestSubmission(assignmentId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("查询成功", submission));
    }

    // =========================================================================
    // GitHub 公开仓库 URL 提交（唯一提交方式）
    // =========================================================================

    /**
     * GitHub 仓库 URL 提交（学生填写公开仓库 URL + 可选分支名）。
     *
     * 执行步骤：
     *  1. 校验请求体 githubUrl 非空
     *  2. GitService.fetchGithubRepoContent：正则解析 → zipball 下载 → 解压 → 合并代码
     *     （失败抛 IllegalArgumentException → GlobalExceptionHandler 返回 400 错误，
     *     学生可看到友好提示并重新提交）
     *  3. 将合并后的 codeContent + 规范化仓库 URL + 实际分支 写入 SubmissionRequest
     *  4. 调用 submissionService.submitCode 持久化
     *  5. 调用 triggerEvaluation 异步受理 AI 评价
     *  6. 返回 202 + submission + evaluationStatus，前端直接进入轮询
     */
    @PostMapping("/github")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitGithub(
            @Valid @RequestBody SubmissionRequest request,
            @AuthenticationPrincipal User user) {
        if (request.getGithubUrl() == null || request.getGithubUrl().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("请填写 GitHub 仓库 URL"));
        }

        // ① 下载 GitHub 仓库 ZIP 并合并代码文本；下载/解析失败直接抛 IllegalArgumentException
        //    （GlobalExceptionHandler → ApiResponse 400，前端 ElMessage 友好提示）
        GitService.FetchedResult fetched = gitService.fetchGithubRepoContent(
                request.getGithubUrl(), request.getGithubBranch());

        // ② 把下载结果回填到 request，复用通用 submitCode 方法
        request.setCodeContent(fetched.mergedCodeContent);
        // 规范化仓库 URL（去掉 /tree/ 等子路径），教师端跳转链接更稳定
        request.setGithubUrl(fetched.normalizedRepoUrl);
        request.setGithubBranch(fetched.actualBranch);
        if (request.getRepositoryUrl() == null || request.getRepositoryUrl().isBlank()) {
            request.setRepositoryUrl(fetched.normalizedRepoUrl);
        }

        CodeSubmission submission = submissionService.submitCode(request, user.getId());
        EvaluationStatusDTO evalStatus = triggerEvaluation(submission.getId());

        Map<String, Object> payload = packSubmitResponse(submission, evalStatus);
        // 把仓库相关附加信息也返回给前端，便于即时展示（如 "从 xx 仓库 xxx 分支抽取了 xx 个文件"）
        payload.put("githubInfo", Map.of(
                "owner", defaultStr(fetched.owner),
                "repo", defaultStr(fetched.repo),
                "branch", defaultStr(fetched.actualBranch),
                "url", defaultStr(fetched.normalizedRepoUrl),
                "fileCount", fetched.fileCount,
                "skippedCount", fetched.skippedCount
        ));

        return ResponseEntity.accepted()
                .body(ApiResponse.success(String.format(
                        "GitHub 仓库代码提交成功：抽取 %d 个源代码文件，评价任务已受理",
                        fetched.fileCount), payload));
    }

    // =========================================================================
    // 私有工具：提交后触发异步评价 + 打包统一响应结构
    // =========================================================================

    /**
     * 评价受理逻辑（Phase 2 异步受理 + 轮询模式）。
     * createPendingEvaluation 具有幂等性：COMPLETED 不会重复投递；FAILED 会重置为 PENDING 再执行。
     */
    private EvaluationStatusDTO triggerEvaluation(Long submissionId) {
        EvaluationResult pending = evaluationService.createPendingEvaluation(submissionId);
        if (EvaluationResult.STATUS_PENDING.equals(pending.getStatus())) {
            evaluationService.executeEvaluationAsync(pending.getId(), submissionId);
        }
        return EvaluationStatusDTO.fromEntity(pending, submissionId);
    }

    /** 提交响应统一格式：submission 实体 + 评价受理状态（便于前端直接开始轮询） */
    private Map<String, Object> packSubmitResponse(CodeSubmission submission, EvaluationStatusDTO evalStatus) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", submission.getId());
        payload.put("assignmentId", submission.getAssignment() != null ? submission.getAssignment().getId() : null);
        payload.put("studentId", submission.getStudent() != null ? submission.getStudent().getId() : null);
        payload.put("submitCount", submission.getSubmitCount());
        payload.put("submittedAt", submission.getSubmittedAt());
        payload.put("githubUrl", submission.getGithubUrl());
        payload.put("githubBranch", submission.getGithubBranch());
        payload.put("repositoryUrl", submission.getRepositoryUrl());
        payload.put("isLatest", submission.getIsLatest());
        payload.put("evaluationStatus", evalStatus);
        return payload;
    }

    private static String defaultStr(String s) {
        return s == null ? "" : s;
    }
}
