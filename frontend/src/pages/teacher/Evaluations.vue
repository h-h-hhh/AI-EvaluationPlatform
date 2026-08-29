<template>
  <div class="evaluations-container">
    <aside class="sidebar">
      <div class="logo">
        <h2>代码评价平台</h2>
      </div>
      <nav class="sidebar-nav">
        <div class="nav-item" @click="$router.push('/teacher')">
          数据看板
        </div>
        <div class="nav-item" @click="$router.push('/teacher/courses')">
          课程管理
        </div>
        <div class="nav-item" @click="$router.push('/teacher/assignments')">
          作业管理
        </div>
        <div class="nav-item active" @click="$router.push('/teacher/evaluations')">
          评价分析
        </div>
      </nav>
      <div class="logout-btn" @click="handleLogout">
        退出登录
      </div>
    </aside>
    
    <main class="main-content">
      <header class="header">
        <div>
          <h1>评价分析</h1>
          <p>查看学生作业评价数据和趋势分析</p>
        </div>
        <div class="header-actions">
          <select v-model="selectedCourse" class="native-select">
            <option value="">全部课程</option>
            <option v-for="course in courses" :key="course.id" :value="course.id">
              {{ course.name }}
            </option>
          </select>
          <select v-model="selectedAssignment" class="native-select">
            <option value="">全部作业</option>
            <option v-for="assignment in assignments" :key="assignment.id" :value="assignment.id">
              {{ assignment.title }}
            </option>
          </select>
        </div>
      </header>
      
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
      
      <!-- 评分分布图 -->
      <div class="charts-row">
        <div class="chart-card">
          <div class="chart-header">
            <h3>评分分布</h3>
          </div>
          <div class="distribution-chart">
            <div class="bar-chart">
              <div v-for="(item, index) in scoreDistribution" :key="index" class="bar-item">
                <div class="bar-wrapper">
                  <div class="bar-fill" :style="{ height: (item.count / maxDistribution) * 100 + '%' }"></div>
                </div>
                <span class="bar-label">{{ item.range }}</span>
                <span class="bar-value">{{ item.count }}人</span>
              </div>
            </div>
          </div>
        </div>
        
        <div class="chart-card">
          <div class="chart-header">
            <h3>维度分析</h3>
          </div>
          <div class="dimension-chart">
            <div class="avg-bars">
              <div class="avg-item">
                <div class="avg-header">
                  <span>正确性</span>
                  <span class="avg-value">{{ dimensionAvg.correctness }}</span>
                </div>
                <div class="avg-bar">
                  <div class="avg-fill" :style="{ width: dimensionAvg.correctness + '%' }"></div>
                </div>
              </div>
              <div class="avg-item">
                <div class="avg-header">
                  <span>代码质量</span>
                  <span class="avg-value">{{ dimensionAvg.quality }}</span>
                </div>
                <div class="avg-bar">
                  <div class="avg-fill quality" :style="{ width: dimensionAvg.quality + '%' }"></div>
                </div>
              </div>
              <div class="avg-item">
                <div class="avg-header">
                  <span>原创性</span>
                  <span class="avg-value">{{ dimensionAvg.originality }}</span>
                </div>
                <div class="avg-bar">
                  <div class="avg-fill originality" :style="{ width: dimensionAvg.originality + '%' }"></div>
                </div>
              </div>
              <div class="avg-item">
                <div class="avg-header">
                  <span>过程性</span>
                  <span class="avg-value">{{ dimensionAvg.process }}</span>
                </div>
                <div class="avg-bar">
                  <div class="avg-fill process" :style="{ width: dimensionAvg.process + '%' }"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 学生评价列表 -->
      <div class="students-section">
        <div class="section-header">
          <h3>学生评价详情</h3>
          <div class="search-box">
            <input v-model="searchKeyword" placeholder="搜索学生姓名..." class="native-input" />
          </div>
        </div>
        
        <div class="students-table">
          <table class="simple-table">
            <thead>
              <tr>
                <th>学生</th>
                <th>Git提交</th>
                <th>评价状态</th>
                <th>正确性</th>
                <th>代码质量</th>
                <th>原创性</th>
                <th>过程性</th>
                <th>总分</th>
                <th>AI检测</th>
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
                  <span :class="student.gitSubmitted ? 'status-success' : 'status-warning'">
                    {{ student.gitSubmitted ? '已提交' : '未提交' }}
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
                  <span class="score-badge" :class="getScoreClass(student.originalityScore)">
                    {{ student.originalityScore }}
                  </span>
                </td>
                <td>
                  <span class="score-badge" :class="getScoreClass(student.processScore)">
                    {{ student.processScore }}
                  </span>
                </td>
                <td>
                  <span class="total-score" :class="getScoreClass(student.totalScore)">
                    {{ student.totalScore }}
                  </span>
                </td>
                <td>
                  <span :class="getAiRiskClass(student.aiRisk)">
                    {{ getAiRiskText(student.aiRisk) }}
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
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { courseApi, assignmentApi, evaluationApi, statisticsApi, authApi } from '../../services/api'
import { useEvaluationPolling } from '../../composables/useEvaluationPolling'

const router = useRouter()

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

const scoreDistribution = ref([
  { range: '90-100', count: 0 },
  { range: '80-89', count: 0 },
  { range: '70-79', count: 0 },
  { range: '60-69', count: 0 },
  { range: '60以下', count: 0 }
])

const maxDistribution = computed(() => {
  return Math.max(...scoreDistribution.value.map(d => d.count), 1)
})

const dimensionAvg = ref({
  correctness: 0,
  quality: 0,
  originality: 0,
  process: 0
})

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
 * TODO(Phase 3): 后端该接口当前返回 LAZY 关联实体，直接序列化存在风险；
 * 待后端改为返回 DTO 后此功能完全生效。接口报错时保留占位数据并提示。
 */
const loadEvaluationStatus = async () => {
  if (!selectedAssignment.value) return
  try {
    const res = await evaluationApi.getByAssignment(selectedAssignment.value)
    if (res?.success && Array.isArray(res.data) && res.data.length > 0) {
      students.value = res.data.map(e => ({
        id: e.id,
        // 注意：后端 submission 为 LAZY 关联，DTO 化之前此字段可能为 null（重试按钮会给出提示）
        submissionId: e.submission?.id ?? null,
        name: e.submission?.student?.name || `提交 #${e.id}`,
        correctnessScore: e.correctnessScore ?? 0,
        qualityScore: e.qualityScore ?? 0,
        originalityScore: e.originalityScore ?? 0,
        processScore: e.processScore ?? 0,
        totalScore: e.finalScore ?? 0,
        // 评价任务状态：驱动"状态"列徽标与重试按钮展示
        evaluationStatus: e.status || 'COMPLETED',
        evaluationError: e.errorMessage || '',
        gitSubmitted: true,
        aiRisk: 'low',
        aiRiskReason: '',
        codeFeatures: ''
      }))
    }
  } catch (error) {
    console.error('加载评价列表失败:', error)
    ElMessage.error('加载评价数据失败，请稍后重试')
  }
}

// 切换"作业"下拉框时刷新评价列表
watch(selectedAssignment, () => {
  loadEvaluationStatus()
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
  alert(`查看学生 ${student.name} 的详细评价报告`)
}

const addFeedback = (student) => {
  alert(`为学生 ${student.name} 添加反馈`)
}

const viewCode = (student) => {
  alert(`查看学生 ${student.name} 的代码`)
}

const contactStudent = (student) => {
  alert(`联系学生 ${student.name}`)
}
</script>

<style scoped>
.evaluations-container {
  display: flex;
  min-height: 100vh;
  background: #f5f5f5;
}

.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #1a73e8 0%, #0d47a1 100%);
  color: white;
  display: flex;
  flex-direction: column;
}

.logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo h2 {
  margin: 0;
  font-size: 18px;
}

.sidebar-nav {
  flex: 1;
  padding: 10px 0;
}

.nav-item {
  padding: 12px 20px;
  cursor: pointer;
  transition: background 0.2s;
}

.nav-item:hover,
.nav-item.active {
  background: rgba(255, 255, 255, 0.1);
}

.logout-btn {
  padding: 15px 20px;
  cursor: pointer;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.main-content {
  flex: 1;
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h1 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.header p {
  margin: 5px 0 0;
  color: #666;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.native-select,
.native-input {
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
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

/* 分布图 */
.bar-chart {
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  height: 200px;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.bar-wrapper {
  width: 40px;
  height: 160px;
  background: #f0f0f0;
  border-radius: 4px;
  display: flex;
  align-items: flex-end;
}

.bar-fill {
  width: 100%;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  border-radius: 4px;
  transition: height 0.3s;
}

.bar-label {
  font-size: 12px;
  color: #666;
}

.bar-value {
  font-size: 12px;
  color: #888;
}

/* 维度图 */
.avg-bars {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.avg-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.avg-header {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #333;
}

.avg-value {
  font-weight: bold;
  color: #667eea;
}

.avg-bar {
  height: 12px;
  background: #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
}

.avg-fill {
  height: 100%;
  background: #667eea;
  border-radius: 6px;
  transition: width 0.3s;
}

.avg-fill.quality { background: #f093fb; }
.avg-fill.originality { background: #4ade80; }
.avg-fill.process { background: #fbbf24; }

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

.search-box .native-input {
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
</style>