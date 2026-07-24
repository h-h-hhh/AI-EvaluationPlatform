package com.example.codeeval.controller;

import com.example.codeeval.service.GitService;
import com.example.codeeval.service.DeepSeekAnalysisService;
import com.example.codeeval.service.CodeQualityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    @Autowired
    private GitService gitService;

    @Autowired
    private DeepSeekAnalysisService deepSeekAnalysisService;

    @Autowired
    private CodeQualityService codeQualityService;

    @GetMapping("/git/submission-history")
    public ResponseEntity<Map<String, Object>> getSubmissionHistory(
            @RequestParam Long studentId,
            @RequestParam Long assignmentId) {
        Map<String, Object> history = gitService.getStudentSubmissionHistory(studentId, assignmentId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/git/repository-analysis")
    public ResponseEntity<Map<String, Object>> getRepositoryAnalysis(@RequestParam Long assignmentId) {
        Map<String, Object> analysis = gitService.getRepositoryAnalysis(assignmentId);
        return ResponseEntity.ok(analysis);
    }

    @PostMapping("/git/validate")
    public ResponseEntity<Map<String, Object>> validateGitUrl(@RequestBody Map<String, String> request) {
        String repoUrl = request.get("repoUrl");
        String repoType = request.get("repoType");
        Map<String, Object> result = gitService.validateGitUrl(repoUrl, repoType);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analyze/code")
    public ResponseEntity<Map<String, Object>> analyzeCode(
            @RequestBody Map<String, Object> request) {
        String code = (String) request.get("code");
        Map<String, Object> context = (Map<String, Object>) request.getOrDefault("context", new HashMap<>());
        Map<String, Object> result = deepSeekAnalysisService.analyzeCode(code, context);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analyze/code-with-deepseek")
    public ResponseEntity<Map<String, Object>> analyzeCodeWithDeepSeek(
            @RequestBody Map<String, Object> request) {
        String code = (String) request.get("code");
        Map<String, Object> context = (Map<String, Object>) request.getOrDefault("context", new HashMap<>());
        Map<String, Object> result = deepSeekAnalysisService.analyzeCodeWithDeepSeek(code, context);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analyze/batch")
    public ResponseEntity<Map<String, Object>> batchAnalyze(
            @RequestBody Map<String, Object> request) {
        List<String> codes = (List<String>) request.get("codes");
        Map<String, Object> context = (Map<String, Object>) request.getOrDefault("context", new HashMap<>());

        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < codes.size(); i++) {
            Map<String, Object> analysis = deepSeekAnalysisService.analyzeCode(codes.get(i), context);
            result.put("analysis_" + i, analysis);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/quality/analyze")
    public ResponseEntity<Map<String, Object>> analyzeQuality(
            @RequestBody Map<String, Object> request) {
        String code = (String) request.get("code");
        String language = (String) request.getOrDefault("language", "java");
        Map<String, Object> result = codeQualityService.analyzeCodeQuality(code, language);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/evaluate/full")
    public ResponseEntity<Map<String, Object>> fullEvaluation(
            @RequestBody Map<String, Object> request) {
        String code = (String) request.get("code");
        String language = (String) request.getOrDefault("language", "java");
        Map<String, Object> context = (Map<String, Object>) request.getOrDefault("context", new HashMap<>());

        Map<String, Object> result = new HashMap<>();

        Map<String, Object> qualityAnalysis = codeQualityService.analyzeCodeQuality(code, language);
        result.put("qualityAnalysis", qualityAnalysis);

        Map<String, Object> llmAnalysis = deepSeekAnalysisService.analyzeCode(code, context);
        result.put("llmAnalysis", llmAnalysis);

        Map<String, Object> scores = (Map<String, Object>) llmAnalysis.get("scores");
        if (scores != null) {
            int qualityScore = (int) qualityAnalysis.get("qualityScore");
            int overallScore = (int) ((Integer) scores.get("correctness") * 0.3 +
                                       qualityScore * 0.3 +
                                       (Integer) scores.get("originality") * 0.2 +
                                       (Integer) scores.get("process") * 0.2);
            result.put("overallScore", overallScore);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "CodeEval Analysis Service");
        return ResponseEntity.ok(status);
    }
}