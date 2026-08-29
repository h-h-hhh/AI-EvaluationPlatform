package com.example.codeeval.controller;

import com.example.codeeval.dto.ApiResponse;
import com.example.codeeval.dto.EvaluationStatusDTO;
import com.example.codeeval.entity.EvaluationResult;
import com.example.codeeval.service.EvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评价控制器
 *
 * Phase 1 变更：评价触发接口从"同步阻塞等待"改为"异步受理 + 轮询"模式
 * - POST /api/evaluations/{submissionId}：秒级受理，返回任务状态（HTTP 202）
 * - GET  /api/evaluations/status/{submissionId}：前端轮询评价进度
 */
@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /**
     * 受理评价请求（异步）
     *
     * 流程：createPendingEvaluation 做幂等校验并确保存在 PENDING 记录，
     * 仅当记录处于 PENDING（新建或失败重试）时才投递异步任务，
     * 避免对同一任务重复投递导致重复执行。
     *
     * 返回 HTTP 202 Accepted：语义为"请求已受理，处理尚未完成"，
     * 前端收到后根据 data.status 开启轮询
     */
    @PostMapping("/{submissionId}")
    public ResponseEntity<ApiResponse<EvaluationStatusDTO>> evaluateSubmission(@PathVariable Long submissionId) {
        // 步骤一：幂等受理（不存在则新建 PENDING；FAILED 则重置为 PENDING；其余直接复用）
        EvaluationResult pending = evaluationService.createPendingEvaluation(submissionId);

        // 步骤二：仅 PENDING 状态需要投递异步任务（幂等命中 PROCESSING/COMPLETED 时跳过）
        if (EvaluationResult.STATUS_PENDING.equals(pending.getStatus())) {
            evaluationService.executeEvaluationAsync(pending.getId(), submissionId);
        }

        // COMPLETED 时提示已有结果，其余情况提示已受理
        String message = EvaluationResult.STATUS_COMPLETED.equals(pending.getStatus())
                ? "该提交已有评价结果"
                : "评价任务已受理";
        return ResponseEntity.accepted()
                .body(ApiResponse.success(message, EvaluationStatusDTO.fromEntity(pending, submissionId)));
    }

    /**
     * 查询评价状态（轮询接口）
     *
     * 前端每隔 3~5 秒调用一次，根据 data.status 判断：
     * - PENDING / PROCESSING → 继续轮询
     * - COMPLETED → 停止轮询，刷新评价结果
     * - FAILED → 停止轮询，展示 errorMessage 并提供重试按钮
     * - data 为 null → 该提交尚未发起评价（展示"待评价"）
     */
    @GetMapping("/status/{submissionId}")
    public ResponseEntity<ApiResponse<EvaluationStatusDTO>> getEvaluationStatus(@PathVariable Long submissionId) {
        EvaluationStatusDTO status = evaluationService.getEvaluationStatus(submissionId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", status));
    }

    // ==================== 查询接口（保持原有功能不变） ====================

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EvaluationResult>> getEvaluationById(@PathVariable Long id) {
        EvaluationResult evaluation = evaluationService.getEvaluationById(id);
        return ResponseEntity.ok(ApiResponse.success("查询成功", evaluation));
    }

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<ApiResponse<EvaluationResult>> getEvaluationBySubmission(@PathVariable Long submissionId) {
        EvaluationResult evaluation = evaluationService.getEvaluationBySubmission(submissionId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", evaluation));
    }

    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<ApiResponse<List<EvaluationResult>>> getEvaluationsByAssignment(@PathVariable Long assignmentId) {
        List<EvaluationResult> evaluations = evaluationService.getEvaluationsByAssignment(assignmentId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", evaluations));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<EvaluationResult>>> getEvaluationsByStudent(@PathVariable Long studentId) {
        List<EvaluationResult> evaluations = evaluationService.getEvaluationsByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", evaluations));
    }
}
