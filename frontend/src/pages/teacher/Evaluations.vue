<template>
  <div class="evaluations-container">
    <AppLayout
      :menu="menu"
      active-path="/teacher/evaluations"
      title="评价分析"
      subtitle="查看学生作业评价数据和趋势分析"
      @logout="handleLogout"
    >
      <template #header-actions>
        <div class="header-actions">
          <el-select v-model="selectedCourse" class="native-select" placeholder="全部课程" size="default">
            <el-option label="全部课程" value="" />
            <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
          </el-select>
          <el-select v-model="selectedAssignment" class="native-select" placeholder="全部作业" size="default">
            <el-option label="全部作业" value="" />
            <el-option v-for="assignment in assignments" :key="assignment.id" :label="assignment.title" :value="assignment.id" />
          </el-select>
        </div>
      </template>

      <!-- 班级整体分析 -->
      <div class="overview-cards">
        <div class="stat-card">
          <div class="stat-icon blue">
            <span>提交</span>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ classStats.submissionRate }}%</span>
            <span class="stat-label">提交率</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon green">
            <span>平均</span>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ classStats.avgScore }}</span>
            <span class="stat-label">平均分</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon purple">
            <span>优秀</span>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ classStats.excellentRate }}%</span>
            <span class="stat-label">优秀率</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon orange">
            <span>待改进</span>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ classStats.improveRate }}%</span>
            <span class="stat-label">待改进率</span>
          </div>
        </div>
      </div>
      
      <!-- 评分分布图（Phase 4：ECharts 柱状图，数据来自 dimension-avg 聚合接口） -->
      <div class="charts-row">
        <div class="chart-card">
          <div class="chart-header">
            <h3>评分分布 <span v-if="stats.studentCount" class="chart-subtitle">（{{ stats.studentCount }} 份已完成评价）</span></h3>
          </div>
          <BaseChart :option="distributionOption" height="260px" />
        </div>

        <div class="chart-card">
          <div class="chart-header">
            <h3>维度分析 <span class="chart-subtitle">（5 维平均分）</span></h3>
          </div>
          <BaseChart :option="dimensionOption" height="260px" />
        </div>
      </div>
      
      <!-- 学生评价列表 -->
      <div class="students-section">
        <div class="section-header">
          <h3>学生评价详情</h3>
          <div class="search-box">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索学生姓名..."
              class="native-input"
              size="default"
              clearable
            />
          </div>
        </div>
        
        <div class="students-table">
          <table class="simple-table">
            <thead>
              <tr>
                <th>学生</th>
                <th>来源 / 仓库</th>
                <th>评价状态</th>
                <th>正确性</th>
                <th>代码质量</th>
                <th>性能</th>
                <th>安全</th>
                <th>总分</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="student in filteredStudents" :key="student.id">
                <td>
                  <div class="student-info">
                    <span class="student-avatar">{{ student.name.charAt(0) }}</span>
                    <span class="student-name">{{ student.name }}</span>
                  </div>
                </td>
                <td>
                  <!-- GitHub 来源：显示 GitHub 图标 + 可点击跳转链接（新标签页）+ 分支名 -->
                  <a v-if="student.githubUrl"
                     :href="student.githubUrl"
                     target="_blank"
                     rel="noopener"
                     class="github-link"
                     :title="'打开 ' + student.githubUrl">
                    <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor" style="vertical-align:-2px;margin-right:4px;"><path d="M8 0C3.58 0 0 3.58 0 8a8 8 0 0 0 5.47 7.59c.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.6 7.6 0 0 1 2-.27c.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z"/></svg>
                    <span class="repo-label">{{ shortenGithubUrl(student.githubUrl) }}</span>
                    <span v-if="student.githubBranch" class="repo-branch">@{{ student.githubBranch }}</span>
                  </a>
                  <!-- 粘贴代码：显示"粘贴代码"徽标（非链接） -->
                  <span v-else class="badge-paste">
                    📝 粘贴代码
                  </span>
                </td>
                <!-- Phase 2 新增：评价任务状态徽标（与后端 PENDING/PROCESSING/COMPLETED/FAILED 对应） -->
                <td>
                  <span class="eval-status" :class="'eval-status-' + (student.evaluationStatus || 'none').toLowerCase()">
                    {{ getStatusText(student.evaluationStatus) }}
                  </span>
                </td>
                <td>
                  <span class="score-badge" :class="getScoreClass(student.correctnessScore)">
                    {{ student.correctnessScore }}
                  </span>
                </td>
                <td>
                  <span class="score-badge" :class="getScoreClass(student.qualityScore)">
                    {{ student.qualityScore }}
                  </span>
                </td>
                <td>
                  <span class="score-badge" :class="getScoreClass(student.performanceScore)">
                    {{ student.performanceScore }}
                  </span>
                </td>
                <td>
                  <span class="score-badge" :class="getScoreClass(student.securityScore)">
                    {{ student.securityScore }}
                  </span>
                </td>
                <td>
                  <span class="total-score" :class="getScoreClass(student.totalScore)">
                    {{ student.totalScore }}
                  </span>
                </td>
                <td>
                  <button class="action-btn" @click="viewDetail(student)">详情</button>
                  <button class="action-btn" @click="addFeedback(student)">反馈</button>
                  <!-- Phase 2 新增：FAILED 记录的重试入口；进行中任务展示禁用态防重复触发 -->
                  <button
                    v-if="student.evaluationStatus === 'FAILED'"
                    class="action-btn retry-btn"
                    :disabled="retryingId === student.id"
                    @click="reevaluate(student)"
                  >
                    {{ retryingId === student.id ? '重试中…' : '重新评价' }}
                  </button>
                  <button
                    v-else-if="student.evaluationStatus === 'PENDING' || student.evaluationStatus === 'PROCESSING'"
                    class="action-btn"
                    disabled
                  >
                    评价中…
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <!-- AI风险学生列表 -->
      <div class="risk-section">
        <div class="section-header">
          <h3>AI风险检测</h3>
          <span class="risk-badge">{{ riskStudents.length }} 名学生需要关注</span>
        </div>
        <div class="risk-cards">
          <div v-for="student in riskStudents" :key="student.id" class="risk-card">
            <div class="risk-header">
              <div class="student-info">
                <span class="student-avatar">{{ student.name.charAt(0) }}</span>
                <span class="student-name">{{ student.name }}</span>
              </div>
              <span class="risk-level" :class="'risk-' + student.aiRisk">
                {{ getAiRiskText(student.aiRisk) }}
              </span>
            </div>
            <div class="risk-details">
              <p><strong>风险原因：</strong>{{ student.aiRiskReason }}</p>
              <p><strong>代码特征：</strong>{{ student.codeFeatures }}</p>
            </div>
            <div class="risk-actions">
              <button class="action-btn" @click="viewCode(student)">查看代码</button>
              <button class="action-btn" @click="contactStudent(student)">联系学生</button>
            </div>
          </div>
        </div>
      </div>
    </AppLayout>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { courseApi, assignmentApi, evaluationApi, statisticsApi, authApi } from '../../services/api'
import { useEvaluationPolling } from '../../composables/useEvaluationPolling'
import BaseChart from '../../components/BaseChart.vue'
import AppLayout from '../../components/AppLayout.vue'

const router = useRouter()

// 教师端侧边栏菜单（与原 template 硬编码一致：label + path，无 icon）
const menu = [
  { label: '数据看板', path: '/teacher' },
  { label: '课程管理', path: '/teacher/courses' },
  { label: '作业管理', path: '/teacher/assignments' },
  { label: '评价分析', path: '/teacher/evaluations' }
]

const selectedCourse = ref('')
const selectedAssignment = ref('')
const searchKeyword = ref('')

const courses = ref([])
const assignments = ref([])
const classStats = ref({
  submissionRate: 0,
  avgScore: 0,
  excellentRate: 0,
  improveRate: 0
})

// ==================== Phase 4：ECharts 图表数据 ====================
// 维度统计聚合数据（评分分布 + 维度平均分共用一次接口请求）
// 默认全 0：作业未选中或接口失败时图表仍可渲染空坐标轴
const stats = ref({
  studentCount: 0,
  averages: {
    correctness: 0,
    quality: 0,
    performance: 0,
    security: 0,
    originality: 0
  },
  distribution: {
    '90-100': 0,
    '80-89': 0,
    '70-79': 0,
    '60-69': 0,
    '<60': 0
  }
})

// 评分分布柱状图配置（分数段 → 人数）
const distributionOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: 40, right: 20, top: 30, bottom: 30 },
  xAxis: {
    type: 'category',
    data: Object.keys(stats.value.distribution),
    axisLabel: { color: '#666', fontSize: 12 }
  },
  yAxis: {
    type: 'value',
    minInterval: 1,          // 人数为整数，避免出现 0.5 刻度
    axisLabel: { color: '#666', fontSize: 12 }
  },
  series: [{
    name: '人数',
    type: 'bar',
    barWidth: '45%',
    data: Object.values(stats.value.distribution),
    itemStyle: {
      // 渐变柱身，与平台紫色系一致
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: '#667eea' },
          { offset: 1, color: '#764ba2' }
        ]
      },
      borderRadius: [4, 4, 0, 0]
    },
    label: { show: true, position: 'top', color: '#666', fontSize: 12 }
  }]
}))

// 维度平均分柱状图配置（5 维均分，上限 100 分）
const dimensionOption = computed(() => {
  // 维度键 → 中文标签映射（与评价模型命名一致）
  const labelMap = {
    correctness: '正确性',
    quality: '代码质量',
    performance: '性能',
    security: '安全性',
    originality: '创新点'
  }
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: {
      type: 'category',
      data: Object.keys(stats.value.averages).map(k => labelMap[k] || k),
      axisLabel: { color: '#666', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      max: 100,              // 统一 100 分制，均分对比更直观
      axisLabel: { color: '#666', fontSize: 12 }
    },
    series: [{
      name: '平均分',
      type: 'bar',
      barWidth: '45%',
      data: Object.values(stats.value.averages),
      itemStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#409eff' },
            { offset: 1, color: '#1890ff' }
          ]
        },
        borderRadius: [4, 4, 0, 0]
      },
      label: { show: true, position: 'top', color: '#666', fontSize: 12 }
    }]
  }
})

/**
 * 加载维度统计聚合数据（Phase 4）
 * 数据源：GET /evaluations/assignment/{id}/dimension-avg
 * 一次请求同时填充"评分分布"与"维度分析"两张柱状图
 */
const loadDimensionStats = async () => {
  if (!selectedAssignment.value) return
  try {
    const res = await evaluationApi.getDimensionStats(selectedAssignment.value)
    if (res?.success && res.data) {
      stats.value = {
        studentCount: res.data.studentCount ?? 0,
        averages: res.data.averages ?? stats.value.averages,
        distribution: res.data.distribution ?? stats.value.distribution
      }
    }
  } catch (error) {
    console.error('加载维度统计失败:', error)
    // 统计加载失败不阻塞评价列表：图表保持当前数据并给出轻提示
    ElMessage.warning('维度统计数据加载失败')
  }
}

const students = ref([])

const filteredStudents = computed(() => {
  if (!searchKeyword.value) return students.value
  return students.value.filter(s => s.name.includes(searchKeyword.value))
})

const riskStudents = computed(() => {
  return students.value.filter(s => s.aiRisk !== 'low')
})

// ==================== Phase 2：评价状态反馈 ====================
// 轮询参数与学生页一致：3s 起步、1.5 倍退避（上限 5s）、最多 60 次判定超时
const { start: startPolling } = useEvaluationPolling({
  interval: 3000,
  maxInterval: 5000,
  maxAttempts: 60
})

// 当前正在重新评价的学生记录ID（驱动按钮 loading，防重复触发）
const retryingId = ref(null)

// 状态文本映射：与后端 EvaluationResult.status 常量保持一致
const getStatusText = (status) => ({
  PENDING: '排队中',
  PROCESSING: '评价中',
  COMPLETED: '已完成',
  FAILED: '失败'
}[status] || '待评价')

/**
 * 加载选中作业的真实评价列表
 * Phase 3：后端已 DTO 化（EvaluationResultDTO），字段全部平铺——
 * submissionId / studentName / 5 维分数 / status / errorMessage 直接可用，
 * 不再访问嵌套 submission 代理对象
 */
const loadEvaluationStatus = async () => {
  if (!selectedAssignment.value) return
  try {
    const res = await evaluationApi.getByAssignment(selectedAssignment.value)
    if (res?.success && Array.isArray(res.data) && res.data.length > 0) {
      students.value = res.data.map(e => ({
        id: e.id,
        // DTO 平铺字段：重试按钮依赖 submissionId 触发重新评价
        submissionId: e.submissionId ?? null,
        name: e.studentName || `提交 #${e.id}`,
        correctnessScore: e.correctnessScore ?? 0,
        qualityScore: e.qualityScore ?? 0,
        performanceScore: e.performanceScore ?? 0,
        securityScore: e.securityScore ?? 0,
        originalityScore: e.originalityScore ?? 0,
        processScore: e.processScore ?? 0,
        totalScore: e.finalScore ?? 0,
        // 评价任务状态：驱动"状态"列徽标与重试按钮展示
        evaluationStatus: e.status || 'COMPLETED',
        evaluationError: e.errorMessage || '',
        // 【GitHub 提交】后端 EvaluationResultDTO 新增字段：
        //   - githubUrl 非空时展示为可点击的"来源/仓库"链接
        //   - githubBranch 显示为 @branch 后缀
        githubUrl: e.githubUrl || null,
        githubBranch: e.githubBranch || null,
        // 向后兼容：若仅填了 repositoryUrl 且包含 github.com，也视作 GitHub 链接
        gitSubmitted: !!(e.githubUrl || e.repositoryUrl)
      }))
    } else {
      students.value = []
    }
  } catch (error) {
    console.error('加载评价列表失败:', error)
    ElMessage.error('加载评价数据失败，请稍后重试')
  }
}

// 切换"作业"下拉框时刷新评价列表与维度统计图表
watch(selectedAssignment, () => {
  loadEvaluationStatus()
  loadDimensionStats()
})

/**
 * FAILED 记录的重新评价入口：
 * 受理接口对 FAILED 记录会重置为 PENDING 重新执行（后端幂等逻辑），
 * 前端随后轮询状态直到 COMPLETED/FAILED
 */
const reevaluate = async (student) => {
  if (!student.submissionId) {
    ElMessage.error('缺少提交记录ID，无法重新评价')
    return
  }
  // 同一时刻只允许一个重新评价任务（轮询 composable 为单实例）
  if (retryingId.value) return

  retryingId.value = student.id
  student.evaluationStatus = 'PENDING'
  try {
    await evaluationApi.evaluate(student.submissionId)
    startPolling(student.submissionId, {
      onUpdate: (data) => {
        student.evaluationStatus = data.status
      },
      onCompleted: (data) => {
        student.evaluationStatus = 'COMPLETED'
        student.totalScore = data.finalScore ?? student.totalScore
        ElMessage.success(`学生 ${student.name} 评价完成`)
        retryingId.value = null
      },
      onFailed: (msg) => {
        student.evaluationStatus = 'FAILED'
        student.evaluationError = msg
        ElMessage.error(`学生 ${student.name} 重新评价失败：${msg}`)
        retryingId.value = null
      }
    })
  } catch (error) {
    console.error('发起重新评价失败:', error)
    student.evaluationStatus = 'FAILED'
    ElMessage.error('发起重新评价失败，请稍后重试')
    retryingId.value = null
  }
}

const loadData = async () => {
  try {
    const [coursesResult, assignmentsResult, statsResult] = await Promise.all([
      courseApi.getAll(),
      assignmentApi.getAll(),
      statisticsApi.getOverview()
    ])
    
    if (coursesResult && coursesResult.success && coursesResult.data) {
      courses.value = coursesResult.data.map(c => ({ id: c.id, name: c.name }))
    }
    if (assignmentsResult && assignmentsResult.success && assignmentsResult.data) {
      assignments.value = assignmentsResult.data.map(a => ({ id: a.id, title: a.title }))
    }
    if (statsResult && statsResult.success && statsResult.data) {
      classStats.value = {
        submissionRate: 0,
        avgScore: statsResult.data.avgScore || 0,
        excellentRate: 0,
        improveRate: 0
      }
    }
    
    students.value = [
      { id: 1, name: '暂无学生数据', correctnessScore: 0, qualityScore: 0, originalityScore: 0, processScore: 0, totalScore: 0, gitSubmitted: false, aiRisk: 'low', aiRiskReason: '', codeFeatures: '', submissionId: null, evaluationStatus: null, evaluationError: '' }
    ]
  } catch (error) {
    console.error('加载评价数据失败:', error)
  }
}

onMounted(() => {
  loadData()
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
  if (score >= 90) return 'excellent'
  if (score >= 80) return 'good'
  if (score >= 60) return 'pass'
  return 'fail'
}

/**
 * 【新增 GitHub 提交功能】教师端"来源 / 仓库"列显示链接时，
 * 把 https://github.com/user/repo 压缩显示为 user/repo，更省空间；
 * 非 GitHub URL 或其它异常格式则直接截断。
 */
const shortenGithubUrl = (url) => {
  if (!url) return '-'
  try {
    const m = /^https?:\/\/github\.com\/([^/]+)\/([^/?#]+)/.exec(url)
    if (m) return `${m[1]}/${m[2]}`
  } catch (_) { /* noop */ }
  return url.length > 40 ? url.slice(0, 37) + '...' : url
}

const getAiRiskClass = (risk) => {
  if (risk === 'high') return 'risk-high'
  if (risk === 'medium') return 'risk-medium'
  return 'risk-low'
}

const getAiRiskText = (risk) => {
  if (risk === 'high') return '高风险'
  if (risk === 'medium') return '中风险'
  return '正常'
}

const viewDetail = (student) => {
  const detail = [
    `学生：${student.name}`,
    `总分：${student.totalScore}`,
    student.githubUrl ? `仓库：${student.githubUrl}${student.githubBranch ? ' @' + student.githubBranch : ''}` : '来源：粘贴代码'
  ].join('\n')
  ElMessage.info({ message: detail, type: 'info', duration: 5000, showClose: true })
}

const addFeedback = (student) => {
  ElMessage.info(`为学生 ${student.name} 添加反馈（占位）`)
}

const viewCode = (student) => {
  ElMessage.info(`查看学生 ${student.name} 的代码（占位）`)
}

const contactStudent = (student) => {
  ElMessage.info(`联系学生 ${student.name}（占位）`)
}
</script>

<style scoped>
.evaluations-container {
  display: flex;
  min-height: 100vh;
  background: #f5f5f5;
}

/* sidebar / header 样式已迁移至 AppLayout.vue */
/* .header-actions 保留在本页：slot 内容由父组件渲染，scoped 样式能作用到 slot 内容 */
.header-actions {
  display: flex;
  gap: 12px;
}

/* el-select / el-input 替换原生控件：外观由 Element Plus 默认主题承担，这里仅保留 class 钩子用于宽度等布局微调 */
.native-select,
.native-input {
  /* 控件自身外观由 Element Plus 默认主题承担 */
}

/* 统计卡片 */
.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
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
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  color: white;
}

.stat-icon.blue { background: #1890ff; }
.stat-icon.green { background: #52c41a; }
.stat-icon.purple { background: #722ed1; }
.stat-icon.orange { background: #fa8c16; }

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-content .stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stat-content .stat-label {
  font-size: 14px;
  color: #888;
}

/* 图表 */
.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.chart-header {
  margin-bottom: 20px;
}

.chart-header h3 {
  margin: 0;
  color: #333;
  font-size: 16px;
}

/* 图表标题右侧的轻量补充说明 */
.chart-subtitle {
  font-size: 12px;
  font-weight: normal;
  color: #999;
}

/* 学生表格 */
.students-section {
  background: white;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  margin: 0;
  color: #333;
}

/* 搜索框宽度：el-input 默认 100%，需显式限定以维持表格标题右侧的紧凑布局 */
.search-box :deep(.el-input) {
  width: 200px;
}

.simple-table {
  width: 100%;
  border-collapse: collapse;
}

.simple-table th,
.simple-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.simple-table th {
  color: #666;
  font-weight: 500;
  font-size: 14px;
}

.student-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.student-avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 14px;
}

.student-name {
  color: #333;
}

.status-success { color: #52c41a; }
.status-warning { color: #fa8c16; }

.score-badge {
  padding: 4px 10px;
  border-radius: 4px;
  font-weight: bold;
  font-size: 13px;
}

.score-badge.excellent { background: #dcfce7; color: #16a34a; }
.score-badge.good { background: #dbeafe; color: #2563eb; }
.score-badge.pass { background: #fef3c7; color: #d97706; }
.score-badge.fail { background: #fee2e2; color: #dc2626; }

.total-score {
  font-weight: bold;
  font-size: 16px;
}

.action-btn {
  padding: 6px 12px;
  border: 1px solid #ddd;
  background: white;
  cursor: pointer;
  border-radius: 4px;
  font-size: 12px;
  margin-right: 4px;
}

.action-btn:hover {
  background: #f5f5f5;
  border-color: #409eff;
  color: #409eff;
}

/* ===== Phase 2：评价状态徽标 ===== */
.eval-status {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

/* 各状态配色：排队=黄、评价中=蓝、完成=绿、失败=红、未发起=灰 */
.eval-status-pending { background: #fef3c7; color: #d97706; }
.eval-status-processing { background: #dbeafe; color: #2563eb; }
.eval-status-completed { background: #dcfce7; color: #16a34a; }
.eval-status-failed { background: #fee2e2; color: #dc2626; }
.eval-status-none { background: #f3f4f6; color: #9ca3af; }

/* 重试按钮：红色描边强调"需要处理"，禁用态降低透明度 */
.retry-btn {
  border-color: #f56c6c;
  color: #f56c6c;
}

.retry-btn:hover {
  background: #fef0f0;
  border-color: #f56c6c;
  color: #f56c6c;
}

.retry-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* AI风险 */
.risk-section {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.risk-badge {
  padding: 6px 12px;
  background: #fff2f0;
  color: #ff4d4f;
  border-radius: 4px;
  font-size: 14px;
}

.risk-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-top: 20px;
}

.risk-card {
  background: #f9f9f9;
  padding: 16px;
  border-radius: 8px;
  border-left: 4px solid #ff4d4f;
}

.risk-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.risk-level {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.risk-high { background: #fee2e2; color: #dc2626; }
.risk-medium { background: #fef3c7; color: #d97706; }
.risk-low { background: #dcfce7; color: #16a34a; }

.risk-details p {
  margin: 0 0 8px;
  font-size: 13px;
  color: #666;
}

.risk-details strong {
  color: #333;
}

.risk-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

/* ===== GitHub 提交：教师端表格"来源/仓库"列链接样式 ===== */
.github-link {
  color: #2563eb;
  text-decoration: none;
  font-size: 13px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  display: inline-flex;
  align-items: center;
  max-width: 240px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.github-link:hover {
  text-decoration: underline;
  color: #1d4ed8;
}
.repo-label {
  font-weight: 600;
}
.repo-branch {
  color: #9ca3af;
  font-size: 12px;
  margin-left: 2px;
  font-weight: normal;
}
.badge-paste {
  display: inline-block;
  padding: 2px 8px;
  background: #f3f4f6;
  color: #4b5563;
  border-radius: 4px;
  font-size: 12px;
}
.req { color: #dc2626; margin-left: 2px; }
</style>