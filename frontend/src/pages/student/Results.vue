<template>
  <div class="results-container">
    <AppLayout
      :menu="menu"
      active-path="/student/results"
      title="评价结果"
      subtitle="查看您的代码作业评价详情"
      @logout="handleLogout"
    >
      <div class="stats-overview">
        <div class="stat-card">
          <div class="stat-icon">📝</div>
          <div class="stat-info">
            <p class="stat-value">{{ totalSubmissions }}</p>
            <p class="stat-label">总提交数</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">✅</div>
          <div class="stat-info">
            <p class="stat-value">{{ evaluatedCount }}</p>
            <p class="stat-label">已评价</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">📊</div>
          <div class="stat-info">
            <p class="stat-value">{{ averageScore }}</p>
            <p class="stat-label">平均分</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🏆</div>
          <div class="stat-info">
            <p class="stat-value">{{ highestScore }}</p>
            <p class="stat-label">最高分</p>
          </div>
        </div>
      </div>
      
      <div class="filter-bar">
        <el-select v-model="selectedCourse" class="native-select" placeholder="全部课程" size="default">
          <el-option label="全部课程" value="" />
          <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
        </el-select>
        <el-select v-model="statusFilter" class="native-select" placeholder="全部状态" size="default">
          <el-option label="全部状态" value="" />
          <el-option label="已评价" value="已评价" />
          <el-option label="待评价" value="待评价" />
        </el-select>
      </div>
      
      <div class="results-list">
        <div v-for="result in filteredResults" :key="result.id" class="result-card">
          <div class="result-header">
            <div class="result-info">
              <h3>{{ result.assignmentTitle }}</h3>
              <p>{{ result.courseName }} - {{ result.submittedTime }}</p>
              <!-- 【GitHub 提交】展示来源：粘贴代码 / GitHub 链接可点击跳转 -->
              <p v-if="result.githubUrl" class="github-source">
                <a :href="result.githubUrl" target="_blank" rel="noopener" class="gh-link" :title="'打开 ' + result.githubUrl">
                  <svg width="13" height="13" viewBox="0 0 16 16" fill="currentColor" style="vertical-align:-1px;margin-right:4px;"><path d="M8 0C3.58 0 0 3.58 0 8a8 8 0 0 0 5.47 7.59c.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.6 7.6 0 0 1 2-.27c.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z"/></svg>
                  GitHub 仓库：{{ result.githubUrl }}
                  <span v-if="result.githubBranch" class="gh-branch">@{{ result.githubBranch }}</span>
                </a>
              </p>
              <p v-else class="paste-source">📝 来源：粘贴代码</p>
            </div>
            <!-- 总评分：dashboard 半圆环（横向占位，不增加 header 高度），环内显示整数分。
                 分数缺失（null/undefined/NaN/空串）时走占位分支，与"真 0 分"区分开。 -->
            <el-progress
              v-if="hasScore(result.finalScore)"
              type="dashboard"
              :percentage="clampScore(result.finalScore)"
              :width="110"
              :stroke-width="10"
              :color="getScoreColor(result.finalScore)"
            >
              <template #default="{ percentage }">
                <span class="score-num">{{ percentage }}</span>
                <span class="score-total">/100</span>
              </template>
            </el-progress>
            <div v-else class="score-missing" title="暂无评分数据">—</div>
          </div>
          
          <div class="scores-grid">
            <div class="score-item">
              <div class="score-label">正确性</div>
              <div class="score-bar">
                <div class="score-fill" :style="{ width: result.scores.correctness + '%' }"></div>
              </div>
              <div class="score-value">{{ result.scores.correctness }}</div>
            </div>
            <div class="score-item">
              <div class="score-label">代码质量</div>
              <div class="score-bar">
                <div class="score-fill" :style="{ width: result.scores.quality + '%' }"></div>
              </div>
              <div class="score-value">{{ result.scores.quality }}</div>
            </div>
            <div class="score-item">
              <div class="score-label">性能优化</div>
              <div class="score-bar">
                <div class="score-fill" :style="{ width: result.scores.performance + '%' }"></div>
              </div>
              <div class="score-value">{{ result.scores.performance }}</div>
            </div>
            <div class="score-item">
              <div class="score-label">安全性</div>
              <div class="score-bar">
                <div class="score-fill" :style="{ width: result.scores.security + '%' }"></div>
              </div>
              <div class="score-value">{{ result.scores.security }}</div>
            </div>
            <div class="score-item">
              <div class="score-label">创新点</div>
              <div class="score-bar">
                <div class="score-fill" :style="{ width: result.scores.originality + '%' }"></div>
              </div>
              <div class="score-value">{{ result.scores.originality }}</div>
            </div>
          </div>
          
          <!-- Phase 4：5 维能力雷达图（真实 DTO 分数驱动，ECharts 按需引入） -->
          <div class="radar-section">
            <h4>能力雷达图</h4>
            <BaseChart :option="buildRadarOption(result.scores)" height="280px" />
          </div>

          <div class="summary-section">
            <h4>综合评价</h4>
            <p>{{ result.summary }}</p>
          </div>
          
          <div class="suggestions-section">
            <h4>改进建议</h4>
            <ul>
              <li v-for="(suggestion, index) in result.suggestions" :key="index">
                <span class="suggestion-num">{{ index + 1 }}</span>
                {{ suggestion }}
              </li>
            </ul>
          </div>
          
          <!-- 维度评语：el-collapse 折叠面板；activeDimensions 默认含全部 5 维，默认全展开 -->
          <el-collapse v-model="activeDimensions" class="dimension-collapse">
            <el-collapse-item title="正确性评价" name="correctness">
              <p class="detail-comment">{{ result.details.correctness.comment }}</p>
            </el-collapse-item>
            <el-collapse-item title="代码质量评价" name="quality">
              <p class="detail-comment">{{ result.details.quality.comment }}</p>
            </el-collapse-item>
            <el-collapse-item title="性能评价" name="performance">
              <p class="detail-comment">{{ result.details.performance.comment }}</p>
            </el-collapse-item>
            <el-collapse-item title="安全性评价" name="security">
              <p class="detail-comment">{{ result.details.security.comment }}</p>
            </el-collapse-item>
            <el-collapse-item title="创新性评价" name="originality">
              <p class="detail-comment">{{ result.details.originality.comment }}</p>
            </el-collapse-item>
          </el-collapse>
          
          <div class="result-actions">
            <button class="action-btn" @click="viewCode(result)">查看代码</button>
            <button class="action-btn" @click="resubmit(result)">重新提交</button>
          </div>
        </div>
        
        <div v-if="filteredResults.length === 0" class="empty-state">
          <div class="empty-icon">📊</div>
          <p>暂无评价结果</p>
          <p class="empty-hint">提交作业后将自动进行评价</p>
        </div>
      </div>
    </AppLayout>

    <div v-if="showCodeModal" class="modal-overlay" @click="showCodeModal = false">
      <div class="code-modal" @click.stop>
        <div class="modal-header">
          <h3>提交代码</h3>
          <button class="close-btn" @click="showCodeModal = false">×</button>
        </div>
        <div class="code-content">
          <pre><code>{{ currentCode }}</code></pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { courseApi, authApi, evaluationApi } from '../../services/api'
import BaseChart from '../../components/BaseChart.vue'
import AppLayout from '../../components/AppLayout.vue'

const router = useRouter()

// 学生端侧边栏菜单（与原 template 硬编码一致：label + path，无 icon）
const menu = [
  { label: '学习看板', path: '/student' },
  { label: '我的课程', path: '/student/courses' },
  { label: '作业提交', path: '/student/submissions' },
  { label: '评价结果', path: '/student/results' }
]

const selectedCourse = ref('')
const statusFilter = ref('')
const showCodeModal = ref(false)
const currentCode = ref('')
// 维度评语折叠面板的展开项：默认含全部 5 维（键名与 scores / details 一致），默认全展开
const activeDimensions = ref(['correctness', 'quality', 'performance', 'security', 'originality'])

const courses = ref([])
const results = ref([])
// 当前登录用户（从 sessionStorage 恢复，与 Submissions.vue 等页面保持一致）
const currentUser = ref(null)

const loadCourses = async () => {
  try {
    const response = await courseApi.getEnrolled()
    if (response && response.success && response.data) {
      courses.value = response.data.map(c => ({ id: c.id, name: c.name }))
    }
  } catch (error) {
    console.error('加载课程失败:', error)
  }
}

/**
 * 加载评价结果列表
 *
 * Phase 3 重构说明（旧版问题）：
 * - 旧版从 submissionApi.getByStudent() 读取 r.evaluation.scores，
 *   但后端提交实体没有嵌套 evaluation 字段 → 分数永远是 0
 * - 旧版调用时未传 studentId（URL 变成 /student/undefined）→ 必然 404
 * - 接口失败时用 generateMockResults() 渲染硬编码假数据
 *
 * 新版数据源：evaluationApi.getByStudent(studentId) 返回 EvaluationResultDTO
 * （5 维分数 / finalScore / feedback / suggestions / codeContent 均为后端真实数据），
 * 删除 mock 兜底，失败时显示错误提示与空状态
 */
const loadResults = async () => {
  if (!currentUser.value?.id) {
    ElMessage.error('登录状态失效，请重新登录')
    return
  }
  try {
    const response = await evaluationApi.getByStudent(currentUser.value.id)
    if (response && response.success && Array.isArray(response.data)) {
      results.value = response.data
        // 结果页只展示已完成评价（PENDING/PROCESSING 在提交页由轮询反馈，FAILED 可重试）
        .filter(e => e.status === 'COMPLETED')
        .map(e => ({
          id: e.id,
          assignmentId: e.assignmentId,
          assignmentTitle: e.assignmentTitle || '未知作业',
          courseName: e.courseName || '',
          submittedTime: formatTime(e.evaluatedAt),
          finalScore: e.finalScore ?? 0,
          // 5 维分数：模板与图表直接使用（键名与 DeepSeek 维度一致）
          scores: {
            correctness: e.correctnessScore ?? 0,
            quality: e.qualityScore ?? 0,
            performance: e.performanceScore ?? 0,
            security: e.securityScore ?? 0,
            originality: e.originalityScore ?? 0
          },
          summary: e.feedback || '暂无评价',
          suggestions: e.suggestions || [],
          // 2026-08-31 修复：直接使用后端 EvaluationResultDTO 解析好的 dimensionComments，
          // 不再在前端二次解析 aiAnalysis（维度评语全为"-"的根因）
          details: buildDetailsFromDTO(e.dimensionComments),
          codeContent: e.codeContent || '',
          status: '已评价',
          // 【GitHub 提交】展示仓库链接：EvaluationResultDTO 新增字段由后端透传
          githubUrl: e.githubUrl || null,
          githubBranch: e.githubBranch || null
        }))
    } else {
      results.value = []
    }
  } catch (error) {
    console.error('加载评价结果失败:', error)
    ElMessage.error('加载评价结果失败，请稍后重试')
    results.value = []
  }
}

/**
 * 将后端 DTO 的 dimensionComments Map 组装成模板需要的 {correctness:{comment}} 结构。
 * - 后端没提供对应维度 → 用 '-' 占位，避免再次显示全是"-"
 * - 模板引用路径：result.details.<维度>.comment，不能直接用 Map
 */
const buildDetailsFromDTO = (dimensionComments) => {
  const keys = ['correctness', 'quality', 'performance', 'security', 'originality']
  const comments = dimensionComments && typeof dimensionComments === 'object' ? dimensionComments : {}
  const result = {}
  for (const k of keys) {
    const c = comments[k]
    result[k] = { comment: (typeof c === 'string' && c.trim()) ? c : '-' }
  }
  return result
}

/**
 * Phase 4：构建 5 维能力雷达图的 ECharts 配置
 *
 * 数据来源：result.scores（EvaluationResultDTO 的 5 维分数映射，非 mock）
 * 维度名与 scores-grid 的展示标签保持一致；indicator 上限 100 分
 */
const buildRadarOption = (scores) => {
  const s = scores || {}
  return {
    tooltip: {},
    radar: {
      // 5 个维度轴：name 展示中文标签，max 统一为 100 分制
      indicator: [
        { name: '正确性', max: 100 },
        { name: '代码质量', max: 100 },
        { name: '性能优化', max: 100 },
        { name: '安全性', max: 100 },
        { name: '创新点', max: 100 }
      ],
      radius: '65%',
      splitNumber: 4,       // 分成 4 档（25/50/75/100 参考线）
      axisName: { color: '#666', fontSize: 12 },
      splitArea: {
        areaStyle: { color: ['#fafafa', '#f0f4fa'] }  // 交替底色提升可读性
      }
    },
    series: [{
      type: 'radar',
      data: [{
        value: [
          s.correctness ?? 0,
          s.quality ?? 0,
          s.performance ?? 0,
          s.security ?? 0,
          s.originality ?? 0
        ],
        name: '维度得分',
        // 面积填充 + 描边样式，与平台蓝色主题一致
        areaStyle: { color: 'rgba(64, 158, 255, 0.25)' },
        lineStyle: { color: '#409eff', width: 2 },
        itemStyle: { color: '#409eff' },
        label: {
          show: true,          // 顶点直接标出分数值
          color: '#409eff',
          fontSize: 11,
          formatter: (p) => p.value
        }
      }]
    }]
  }
}

// 评价时间格式化（ISO 字符串 → 本地可读格式）
const formatTime = (iso) => (iso ? new Date(iso).toLocaleString('zh-CN') : '-')

onMounted(() => {
  // 先恢复当前登录用户（loadResults 依赖 user.id 拼接学生维度的评价查询）
  const userStr = sessionStorage.getItem('user')
  if (userStr) {
    currentUser.value = JSON.parse(userStr)
  }
  loadCourses()
  loadResults()
})


const filteredResults = computed(() => {
  let result = results.value
  if (selectedCourse.value) {
    result = result.filter(r => r.courseName)
  }
  if (statusFilter.value) {
    result = result.filter(r => r.status === statusFilter.value)
  }
  return result
})

const totalSubmissions = computed(() => results.value.length)

const evaluatedCount = computed(() => 
  results.value.filter(r => r.status === '已评价').length
)

const averageScore = computed(() => {
  const evaluated = results.value.filter(r => r.status === '已评价')
  if (evaluated.length === 0) return '-'
  const total = evaluated.reduce((sum, r) => sum + r.finalScore, 0)
  return Math.round(total / evaluated.length)
})

const highestScore = computed(() => {
  const evaluated = results.value.filter(r => r.status === '已评价')
  if (evaluated.length === 0) return '-'
  return Math.max(...evaluated.map(r => r.finalScore))
})

const handleLogout = async () => {
  try {
    await authApi.logout()
  } catch (e) {
    console.error(e)
  }
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('role')
  sessionStorage.removeItem('user')
  router.push('/')
}

const getScoreClass = (score) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 60) return 'score-pass'
  return 'score-fail'
}

/**
 * 分数是否有效：null / undefined / 空串 / NaN 均视为"数据缺失"。
 * 用于区分"缺失"（显示 —）与"真的 0 分"（显示 0/100 红色环）。
 */
const hasScore = (score) => {
  if (score === null || score === undefined || score === '') return false
  return !Number.isNaN(Number(score))
}

/**
 * 分数钳制：el-progress 的 percentage 要求 0~100，越界会在控制台告警。
 * 仅用于展示层（percentage / color），不改动 result.finalScore 原始值。
 */
const clampScore = (score) => Math.min(100, Math.max(0, Number(score) || 0))

/**
 * 环形进度主色映射：复用 getScoreClass 的 90/80/60 阈值，
 * 避免出现第二份分数段判断逻辑。四色与 Element Plus 语义色一致。
 */
const getScoreColor = (score) => ({
  'score-excellent': '#67c23a',
  'score-good': '#409eff',
  'score-pass': '#e6a23c',
  'score-fail': '#f56c6c'
}[getScoreClass(score)] || '#909399')

const viewCode = (result) => {
  currentCode.value = result.codeContent
  showCodeModal.value = true
}

const resubmit = (result) => {
  router.push(`/student/submissions`)
}
</script>

<style scoped>
.results-container {
  display: flex;
  min-height: 100vh;
  background: #f5f5f5;
}

/* sidebar / header 样式已迁移至 AppLayout.vue */

.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  font-size: 32px;
}

.stat-info .stat-value {
  margin: 0;
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.stat-info .stat-label {
  margin: 5px 0 0;
  font-size: 14px;
  color: #666;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

/* el-select 替换原生 select：仅保留 min-width 维持筛选栏布局，其余外观由 Element Plus 默认主题承担 */
.native-select {
  min-width: 150px;
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.result-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.result-header .result-info h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

.result-header .result-info p {
  margin: 5px 0 0;
  color: #666;
  font-size: 14px;
}

/* 总评分改为 el-progress dashboard 半圆环后，
   原 .score-badge / .score-excellent / .score-good / .score-pass / .score-fail 五条样式已无引用，删除。
   主色改由 getScoreColor() 以 el-progress 的 color prop 控制。 */
.score-num {
  font-size: 22px;
  font-weight: bold;
  color: #333;
  line-height: 1.1;
}

.score-total {
  font-size: 12px;
  color: #909399;
}

/* 分数缺失占位：与"真 0 分"的红色环区分开 */
.score-missing {
  min-width: 110px;
  text-align: center;
  font-size: 28px;
  font-weight: bold;
  color: #c0c4cc;
  line-height: 1;
}

.scores-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

/* Phase 4：能力雷达图区域 */
.radar-section {
  background: #f9fbff;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.radar-section h4 {
  margin: 0 0 8px;
  color: #333;
  font-size: 14px;
}

.score-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.score-label {
  font-size: 12px;
  color: #666;
  text-align: center;
}

.score-bar {
  height: 8px;
  background: #eee;
  border-radius: 4px;
  overflow: hidden;
}

.score-fill {
  height: 100%;
  background: linear-gradient(90deg, #409eff 0%, #66b1ff 100%);
  border-radius: 4px;
  transition: width 0.5s ease;
}

.score-value {
  font-size: 14px;
  font-weight: bold;
  text-align: center;
  color: #333;
}

.summary-section {
  background: #f9f9f9;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.summary-section h4 {
  margin: 0 0 8px;
  color: #333;
  font-size: 14px;
}

.summary-section p {
  margin: 0;
  color: #666;
  line-height: 1.6;
}

.suggestions-section {
  margin-bottom: 20px;
}

.suggestions-section h4 {
  margin: 0 0 12px;
  color: #333;
  font-size: 14px;
}

.suggestions-section ul {
  margin: 0;
  padding: 0;
  list-style: none;
}

.suggestions-section li {
  padding: 10px 15px;
  background: #fff7e6;
  margin-bottom: 8px;
  border-radius: 6px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.suggestion-num {
  background: #ffa940;
  color: white;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}

/* 维度评语改为 el-collapse 折叠面板：
   原 .detail-sections(2列 grid) / .detail-section(卡片底色与内边距) / .detail-section h4 三条样式已无引用，删除。
   el-collapse-item 自带边框与标题样式，仅需控制外层间距与正文排版。 */
.dimension-collapse {
  margin-bottom: 20px;
}

.detail-comment {
  margin: 0;
  color: #666;
  font-size: 13px;
  line-height: 1.5;
}

.result-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.action-btn {
  padding: 8px 16px;
  border: 1px solid #409eff;
  background: white;
  color: #409eff;
  cursor: pointer;
  border-radius: 4px;
  font-size: 14px;
}

.action-btn:hover {
  background: #e6f7ff;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state p {
  margin: 0 0 8px;
  color: #666;
}

.empty-state .empty-hint {
  color: #888;
  font-size: 14px;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.code-modal {
  background: white;
  border-radius: 12px;
  width: 80%;
  max-height: 80%;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.close-btn:hover {
  color: #333;
}

.code-content {
  padding: 20px;
  overflow: auto;
  max-height: 60vh;
}

.code-content pre {
  margin: 0;
  background: #1e1e1e;
  padding: 20px;
  border-radius: 8px;
  overflow-x: auto;
}

.code-content code {
  color: #d4d4d4;
  font-family: monospace;
  font-size: 14px;
  line-height: 1.6;
}

/* 【GitHub 提交】学生结果页展示仓库链接与粘贴来源 */
.github-source, .paste-source {
  margin: 4px 0 0 !important;
  font-size: 13px !important;
}
.github-source .gh-link {
  color: #2563eb;
  text-decoration: none;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}
.github-source .gh-link:hover { text-decoration: underline; color: #1d4ed8; }
.github-source .gh-branch {
  color: #6b7280;
  font-size: 12px;
  margin-left: 2px;
  font-family: ui-monospace, Menlo, Consolas, monospace;
}
.paste-source {
  color: #6b7280;
  font-family: inherit;
}

/* Submissions.vue 共用样式：GitHub Tab hint 文字、必填星号、解析信息面板 */
.req { color: #dc2626; margin-left: 2px; }
.hint { color: #6b7280; font-size: 12px; margin-top: 4px; display: block; }
.github-submit-info {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 6px;
  background: #ecfdf5; border: 1px solid #6ee7b7; margin-bottom: 12px;
}
.github-submit-info .status-icon.success { color: #059669; font-weight: bold; }
.github-submit-info .status-text p { margin: 0; font-size: 13px; color: #065f46; }
</style>