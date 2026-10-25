package com.example.codeeval.dto;

import com.example.codeeval.entity.EvaluationResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 作业维度统计 DTO（Phase 4：教师端图表数据源）
 *
 * 由 EvaluationService.getDimensionStats 聚合生成，包含三部分：
 * 1. studentCount   参与统计的已完成评价数量（PENDING/PROCESSING/FAILED 不计入）
 * 2. averages       5 个维度的平均分（correctness/quality/performance/security/originality）
 * 3. distribution   总分分数段分布（90-100 / 80-89 / 70-79 / 60-69 / <60）
 *
 * 使用 Map 而非平铺字段的原因：维度键名与 DeepSeek 评分模型、前端图表配置
 * 直接对应，前端可遍历生成 ECharts 的 indicator / xAxis，新增维度时无需改 DTO
 */
public class DimensionStatsDTO {

    /** 参与统计的已完成评价数量 */
    private int studentCount;

    /** 5 维平均分，键名与实体评分字段对应 */
    private Map<String, Double> averages;

    /** 总分分数段人数分布，键为分数段标签 */
    private Map<String, Long> distribution;

    public DimensionStatsDTO() {
    }

    public DimensionStatsDTO(int studentCount, Map<String, Double> averages, Map<String, Long> distribution) {
        this.studentCount = studentCount;
        this.averages = averages;
        this.distribution = distribution;
    }

    /**
     * 从已完成评价实体列表聚合统计（纯内存计算，无需额外 SQL）
     *
     * @param completedEvaluations 仅含 status=COMPLETED 的评价记录
     */
    public static DimensionStatsDTO fromEvaluations(List<EvaluationResult> completedEvaluations) {
        int count = completedEvaluations.size();

        // ===== 1. 五维平均分（保留 1 位小数） =====
        Map<String, Double> averages = new LinkedHashMap<>();
        if (count > 0) {
            double sumCorrectness = 0, sumQuality = 0, sumPerformance = 0, sumSecurity = 0, sumOriginality = 0;
            for (EvaluationResult e : completedEvaluations) {
                sumCorrectness += nz(e.getCorrectnessScore());
                sumQuality += nz(e.getQualityScore());
                sumPerformance += nz(e.getPerformanceScore());
                sumSecurity += nz(e.getSecurityScore());
                sumOriginality += nz(e.getOriginalityScore());
            }
            averages.put("correctness", round1(sumCorrectness / count));
            averages.put("quality", round1(sumQuality / count));
            averages.put("performance", round1(sumPerformance / count));
            averages.put("security", round1(sumSecurity / count));
            averages.put("originality", round1(sumOriginality / count));
        } else {
            // 无数据时返回 5 个 0 值键，前端图表仍能渲染空坐标轴而不是报错
            averages.put("correctness", 0.0);
            averages.put("quality", 0.0);
            averages.put("performance", 0.0);
            averages.put("security", 0.0);
            averages.put("originality", 0.0);
        }

        // ===== 2. 总分分数段分布（与教师端"评分分布"图表分段一致） =====
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("90-100", completedEvaluations.stream().filter(e -> nz(e.getFinalScore()) >= 90).count());
        distribution.put("80-89", completedEvaluations.stream().filter(e -> nz(e.getFinalScore()) >= 80 && nz(e.getFinalScore()) < 90).count());
        distribution.put("70-79", completedEvaluations.stream().filter(e -> nz(e.getFinalScore()) >= 70 && nz(e.getFinalScore()) < 80).count());
        distribution.put("60-69", completedEvaluations.stream().filter(e -> nz(e.getFinalScore()) >= 60 && nz(e.getFinalScore()) < 70).count());
        distribution.put("<60", completedEvaluations.stream().filter(e -> nz(e.getFinalScore()) < 60).count());

        return new DimensionStatsDTO(count, averages, distribution);
    }

    /** null 分数按 0 处理（历史脏数据兜底） */
    private static double nz(Double v) {
        return v == null ? 0.0 : v;
    }

    /** 保留 1 位小数 */
    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    // ==================== Getter / Setter ====================

    public int getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(int studentCount) {
        this.studentCount = studentCount;
    }

    public Map<String, Double> getAverages() {
        return averages;
    }

    public void setAverages(Map<String, Double> averages) {
        this.averages = averages;
    }

    public Map<String, Long> getDistribution() {
        return distribution;
    }

    public void setDistribution(Map<String, Long> distribution) {
        this.distribution = distribution;
    }
}
