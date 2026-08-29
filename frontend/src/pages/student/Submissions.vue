<template>
  <div class="submissions-container">
    <aside class="sidebar">
      <div class="logo">
        <h2>代码评价平台</h2>
      </div>
      <nav class="sidebar-nav">
        <div class="nav-item" @click="$router.push('/student')">
          学习看板
        </div>
        <div class="nav-item" @click="$router.push('/student/courses')">
          我的课程
        </div>
        <div class="nav-item active" @click="$router.push('/student/submissions')">
          作业提交
        </div>
        <div class="nav-item" @click="$router.push('/student/results')">
          评价结果
        </div>
      </nav>
      <div class="logout-btn" @click="handleLogout">
        退出登录
      </div>
    </aside>
    
    <main class="main-content">
      <header class="header">
        <div>
          <h1>作业提交</h1>
          <p>通过Git提交您的代码作业</p>
        </div>
      </header>
      
      <div class="filter-bar">
        <select v-model="selectedCourse" class="native-select" @change="loadAssignments">
          <option value="">全部课程</option>
          <option v-for="course in courses" :key="course.id" :value="course.id">
            {{ course.name }}
          </option>
        </select>
        <select v-model="statusFilter" class="native-select">
          <option value="">全部状态</option>
          <option value="进行中">进行中</option>
          <option value="已提交">已提交</option>
          <option value="已截止">已截止</option>
        </select>
      </div>
      
      <div class="assignments-list">
        <div v-for="assignment in filteredAssignments" :key="assignment.id" class="assignment-card">
          <div class="assignment-header">
            <div class="assignment-info">
              <h3>{{ assignment.title }}</h3>
              <p>{{ assignment.courseName }}</p>
            </div>
            <span :class="getStatusClass(assignment.status)">
              {{ assignment.status }}
            </span>
          </div>
          
          <div class="assignment-details">
            <div class="detail-item">
              <span>截止日期: {{ formatDeadline(assignment.deadline) }}</span>
            </div>
            <div class="detail-item">
              <span>总分: {{ assignment.totalScore }}分</span>
            </div>
            <div class="detail-item">
              <span>提交次数: {{ assignment.submissionCount || 0 }}次</span>
            </div>
          </div>
          
          <p class="assignment-description">{{ assignment.description }}</p>
          
          <div v-if="assignment.status !== '已截止'" class="submit-section">
            <div class="submit-tabs">
              <button 
                :class="['tab-btn', { active: activeTab === 'git' }]" 
                @click="activeTab = 'git'"
              >
                Git提交
              </button>
              <button 
                :class="['tab-btn', { active: activeTab === 'manual' }]" 
                @click="activeTab = 'manual'"
              >
                手动提交
              </button>
            </div>
            
            <div v-if="activeTab === 'git'" class="git-submit-form">
              <div class="form-group">
                <label>个人Git仓库地址 *</label>
                <input 
                  v-model="assignment.gitUrl" 
                  placeholder="https://github.com/yourname/repo" 
                  class="native-input"
                  :disabled="assignment.status === '已提交'"
                />
              </div>
              <div class="form-group">
                <label>分支名称</label>
                <input 
                  v-model="assignment.branch" 
                  placeholder="main 或 master" 
                  class="native-input"
                  :disabled="assignment.status === '已提交'"
                />
              </div>
              <div class="form-group">
                <label>提交说明</label>
                <textarea 
                  v-model="assignment.commitMessage" 
                  placeholder="描述本次提交的内容" 
                  rows="2" 
                  class="native-textarea"
                  :disabled="assignment.status === '已提交'"
                ></textarea>
              </div>
              
              <div v-if="assignment.status === '已提交'" class="git-status-info">
                <div class="status-icon">
                  <span class="icon-success">✓</span>
                </div>
                <div class="status-text">
                  <p><strong>已提交</strong> - 提交时间: {{ assignment.submittedTime }}</p>
                </div>
              </div>
              
              <div class="submit-actions">
                <button 
                  v-if="assignment.status === '已提交'" 
                  class="warning-btn" 
                  @click="resubmitAssignment(assignment)"
                >
                  重新提交
                </button>
                <button 
                  v-else 
                  class="primary-btn" 
                  @click="submitAssignment(assignment)"
                >
                  提交作业
                </button>
              </div>
            </div>
            
            <div v-else class="manual-submit-form">
              <div class="form-group">
                <label>代码文件</label>
                <div class="file-upload">
                  <input type="file" accept=".zip,.tar.gz,.java,.py,.js" class="file-input" @change="handleFileSelect($event, assignment)" />
                  <span class="file-hint">支持 ZIP、TAR.GZ 或单个源代码文件</span>
                </div>
              </div>
              <div class="form-group">
                <label>代码内容（可直接粘贴）</label>
                <textarea 
                  v-model="assignment.codeContent" 
                  placeholder="在此粘贴您的代码..." 
                  rows="10" 
                  class="native-textarea"
                ></textarea>
              </div>
              <div class="form-group">
                <label>提交说明</label>
                <textarea 
                  v-model="assignment.commitMessage" 
                  placeholder="描述本次提交的内容" 
                  rows="3" 
                  class="native-textarea"
                ></textarea>
              </div>
              <button class="primary-btn" :disabled="assignment.submitting" @click="submitManual(assignment)">
                {{ assignment.submitting ? '提交中…' : '提交代码' }}
              </button>
            </div>
          </div>
          
          <div v-else class="submitted-info">
            <div class="submitted-status">
              <span class="status-icon success">✓</span>
              <div class="status-details">
                <p><strong>作业已截止</strong></p>
                <p>截止时间: {{ formatDeadline(assignment.deadline) }}</p>
              </div>
            </div>
          </div>
          
          <!-- Phase 2：评价状态区域，按异步任务状态展示三种反馈（评价中 / 失败可重试 / 已完成） -->
          <div v-if="assignment.evaluationStatus === 'PENDING' || assignment.evaluationStatus === 'PROCESSING'" class="evaluation-section">
            <div class="eval-header">
              <span class="eval-badge">
                <span class="loading-dot"></span>
                {{ assignment.evaluationStatus === 'PENDING' ? '排队等待评价' : 'AI 评价中' }}
              </span>
              <span class="eval-score">DeepSeek 正在分析代码，预计 1~2 分钟…</span>
            </div>
            <!-- Element Plus indeterminate 进度条：滑动动画表达"进行中"，不承诺具体百分比 -->
            <el-progress
              class="eval-progress"
              :percentage="50"
              :indeterminate="true"
              :duration="2"
              :show-text="false"
            />
          </div>

          <div v-else-if="assignment.evaluationStatus === 'FAILED'" class="evaluation-section evaluation-failed">
            <div class="eval-header">
              <span class="eval-badge eval-badge-failed">评价失败</span>
              <span class="eval-score">{{ assignment.evaluationError || '原因未知，请重试' }}</span>
            </div>
            <!-- 重试按钮：loading 期间禁用，防止重复触发 -->
            <button class="action-btn" :disabled="assignment.retrying" @click="retryEvaluation(assignment)">
              {{ assignment.retrying ? '重试中…' : '重新评价' }}
            </button>
          </div>

          <div v-else-if="assignment.evaluated" class="evaluation-section">
            <div class="eval-header">
              <span class="eval-badge">评价完成</span>
              <span class="eval-score">得分: {{ assignment.finalScore }}</span>
            </div>
            <button class="action-btn" @click="viewEvaluation(assignment)">查看评价详情</button>
          </div>
        </div>
        
        <div v-if="filteredAssignments.length === 0" class="empty-state">
          <div class="empty-icon">📝</div>
          <p>没有找到作业</p>
          <p class="empty-hint">请选择课程查看作业列表</p>
        </div>
      </div>
    </main>
    
    <div v-if="showSuccessToast" class="success-toast">
      <span class="toast-icon">✓</span>
      <span class="toast-message">{{ toastMessage }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { assignmentApi, courseApi, submissionApi, gitApi, evaluationApi, authApi } from '../../services/api'
import { useEvaluationPolling } from '../../composables/useEvaluationPolling'

const router = useRouter()
const route = useRoute()

const selectedCourse = ref('')
const statusFilter = ref('')
const activeTab = ref('git')
const showSuccessToast = ref(false)
const toastMessage = ref('')

const courses = ref([])
const assignments = ref([])
const currentUser = ref(null)

// ==================== Phase 2：AI 评价轮询 ====================
// 轮询参数：3s 起步、1.5 倍退避（上限 5s）、最多 60 次判定超时（约 3~5 分钟）
const { start: startPolling } = useEvaluationPolling({
  interval: 3000,
  maxInterval: 5000,
  maxAttempts: 60
})

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

const loadAssignments = async () => {
  try {
    const response = await assignmentApi.getAll()
    if (response && response.success && response.data) {
      assignments.value = response.data.map(a => ({
        id: a.id,
        title: a.title,
        courseId: a.course?.id || a.courseId,
        courseName: a.course?.name || '',
        deadline: a.deadline,
        totalScore: a.totalScore,
        description: a.description,
        status: getAssignmentStatus(a),
        gitEnabled: true,
        gitUrl: '',
        branch: 'main',
        commitMessage: '',
        codeContent: '',
        submittedTime: null,
        submissionCount: 0,
        evaluated: false,
        finalScore: 0,
        // ===== Phase 2 新增：评价异步链路相关字段 =====
        submissionId: null,      // 后端返回的提交记录ID，评价受理/轮询接口依赖它
        evaluationStatus: null,  // 评价任务状态：PENDING/PROCESSING/COMPLETED/FAILED，null=未发起
        evaluationError: '',     // 评价失败原因（FAILED 时展示）
        submitting: false,       // 提交按钮 loading/防重复标记
        retrying: false          // 重新评价按钮 loading 标记
      }))
    }
  } catch (error) {
    console.error('加载作业失败:', error)
  }
}

const getAssignmentStatus = (assignment) => {
  if (!assignment.status) return '已截止'
  
  const deadline = new Date(assignment.deadline)
  const now = new Date()
  
  if (now > deadline) {
    return '已截止'
  }
  
  return '进行中'
}

onMounted(() => {
  loadCourses()
  loadAssignments()
  
  const courseId = route.query.courseId
  if (courseId) {
    selectedCourse.value = courseId
  }
  
  const userStr = sessionStorage.getItem('user')
  if (userStr) {
    currentUser.value = JSON.parse(userStr)
  }
})

const filteredAssignments = computed(() => {
  let result = assignments.value
  if (selectedCourse.value) {
    result = result.filter(a => a.courseId === parseInt(selectedCourse.value))
  }
  if (statusFilter.value) {
    result = result.filter(a => a.status === statusFilter.value)
  }
  return result
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

const getStatusClass = (status) => {
  if (status === '已截止') return 'status-danger'
  if (status === '已提交') return 'status-success'
  return 'status-warning'
}

const formatDeadline = (deadline) => {
  if (!deadline) return '-'
  return deadline.replace('T', ' ')
}

const showToast = (message) => {
  toastMessage.value = message
  showSuccessToast.value = true
  setTimeout(() => {
    showSuccessToast.value = false
  }, 3000)
}

const submitAssignment = async (assignment) => {
  // 防重复提交：上一次请求未结束时忽略本次点击
  if (assignment.submitting) return

  if (!assignment.gitUrl && !assignment.codeContent) {
    ElMessage.error('请输入Git仓库地址或粘贴代码内容')
    return
  }

  assignment.submitting = true
  try {
    if (assignment.gitUrl) {
      const validateResult = await gitApi.validateUrl({
        repoUrl: assignment.gitUrl,
        repoType: 'github'
      })

      if (!validateResult.valid) {
        ElMessage.error('仓库地址验证失败: ' + (validateResult.message || '未知错误'))
        return
      }
    }

    const submitData = {
      assignmentId: assignment.id,
      repositoryUrl: assignment.gitUrl || '',
      gitCommitHash: '',
      codeContent: assignment.codeContent || '',
      submitType: assignment.gitUrl ? 'GIT' : 'MANUAL'
    }

    const response = await submissionApi.submit(submitData)

    if (response.success) {
      assignment.status = '已提交'
      assignment.submittedTime = new Date().toLocaleString('zh-CN')
      assignment.submissionCount++
      assignment.evaluated = false
      // 保存后端返回的提交记录ID（POST /submissions 返回 CodeSubmission 实体）
      assignment.submissionId = response.data?.id || null
      ElMessage.success(`作业 "${assignment.title}" 提交成功！`)
      // 提交成功后立即发起 AI 评价：后端为异步受理（秒级返回 202），无需 setTimeout 延迟
      await triggerEvaluation(assignment)
    } else {
      ElMessage.error('提交失败: ' + (response.message || '未知错误'))
    }
  } catch (error) {
    console.error('提交作业失败:', error)
    ElMessage.error('提交作业失败，请稍后重试')
  } finally {
    // 无论成败都复位按钮状态（return 分支也会经过 finally）
    assignment.submitting = false
  }
}

// ==================== Phase 2：AI 评价异步链路 ====================
// 旧版 evaluateSubmission（同步调 analysisApi + 失败硬编码 85 分假数据）已删除，
// 改为"受理 + 轮询"模式：POST /evaluations/{submissionId} 秒级受理，
// 前端轮询 GET /evaluations/status/{submissionId} 直到 COMPLETED/FAILED

const triggerEvaluation = async (assignment) => {
  if (!assignment.submissionId) {
    ElMessage.error('缺少提交记录ID，无法发起 AI 评价')
    return
  }

  // 立即进入"排队中"状态，驱动模板展示进度徽标
  assignment.evaluationStatus = 'PENDING'
  assignment.evaluationError = ''

  try {
    // 异步受理：后端立即返回 202 + 当前状态；若幂等命中 COMPLETED 则直接展示结果
    const res = await evaluationApi.evaluate(assignment.submissionId)
    if (res?.data?.status === 'COMPLETED') {
      applyEvaluationCompleted(assignment, res.data)
      return
    }

    // 开启轮询：3s 起步、1.5 倍退避（上限 5s）、最多 60 次判定超时
    startPolling(assignment.submissionId, {
      onUpdate: (data) => {
        // PENDING → PROCESSING 等中间状态变化，实时刷新徽标文案
        assignment.evaluationStatus = data.status
      },
      onCompleted: (data) => applyEvaluationCompleted(assignment, data),
      onFailed: (msg) => {
        assignment.evaluationStatus = 'FAILED'
        assignment.evaluationError = msg
        ElMessage.error(`作业 "${assignment.title}" 评价失败：${msg}`)
      }
    })
  } catch (error) {
    console.error('发起 AI 评价失败:', error)
    assignment.evaluationStatus = 'FAILED'
    assignment.evaluationError = '发起评价请求失败'
    ElMessage.error('发起 AI 评价失败，请检查网络后重试')
  }
}

// 轮询收到 COMPLETED：回填真实分数（替代旧版失败硬编码 85 分的假数据）
const applyEvaluationCompleted = (assignment, data) => {
  assignment.evaluationStatus = 'COMPLETED'
  assignment.evaluated = true
  assignment.finalScore = data.finalScore ?? 0
  ElMessage.success(`作业 "${assignment.title}" AI 评价完成！`)
}

// FAILED 状态的重试入口：重新走受理接口（后端幂等逻辑会把 FAILED 记录重置为 PENDING 重新执行）
const retryEvaluation = async (assignment) => {
  assignment.retrying = true
  try {
    await triggerEvaluation(assignment)
  } finally {
    assignment.retrying = false
  }
}

const submitManual = async (assignment) => {
  if (!assignment.codeContent) {
    alert('请输入代码内容')
    return
  }
  
  await submitAssignment(assignment)
}

const handleFileSelect = (event, assignment) => {
  const file = event.target.files[0]
  if (file) {
    const reader = new FileReader()
    reader.onload = (e) => {
      if (file.name.endsWith('.java') || file.name.endsWith('.py') || file.name.endsWith('.js')) {
        assignment.codeContent = e.target.result
      } else {
        alert('暂不支持该文件格式的自动读取，请手动上传或粘贴代码')
      }
    }
    reader.readAsText(file)
  }
}

const resubmitAssignment = (assignment) => {
  assignment.status = '进行中'
  assignment.submittedTime = null
  assignment.evaluated = false
  // 重置评价状态：重新提交后会基于新的提交记录再次发起评价
  assignment.evaluationStatus = null
  assignment.evaluationError = ''
  showToast(`请重新提交作业 "${assignment.title}"`)
}

const viewEvaluation = (assignment) => {
  router.push(`/student/results`)
}
</script>

<style scoped>
.submissions-container {
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

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.native-select {
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  min-width: 150px;
}

.assignments-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.assignment-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.assignment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.assignment-header .assignment-info h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

.assignment-header .assignment-info p {
  margin: 5px 0 0;
  color: #666;
  font-size: 14px;
}

.status-danger {
  color: #ff4d4f;
  padding: 4px 12px;
  background: #fff2f0;
  border-radius: 4px;
  font-size: 14px;
}

.status-success {
  color: #52c41a;
  padding: 4px 12px;
  background: #f6ffed;
  border-radius: 4px;
  font-size: 14px;
}

.status-warning {
  color: #faad14;
  padding: 4px 12px;
  background: #fffbe6;
  border-radius: 4px;
  font-size: 14px;
}

.assignment-details {
  display: flex;
  gap: 24px;
  margin-bottom: 12px;
}

.assignment-details .detail-item {
  color: #666;
  font-size: 14px;
}

.assignment-description {
  margin: 0 0 20px;
  color: #666;
  line-height: 1.6;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 8px;
}

.submit-section {
  border-top: 1px solid #eee;
  padding-top: 20px;
}

.submit-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.tab-btn {
  padding: 10px 20px;
  border: 1px solid #ddd;
  background: white;
  cursor: pointer;
  border-radius: 4px;
  font-size: 14px;
  transition: all 0.2s;
}

.tab-btn:hover {
  background: #f5f5f5;
}

.tab-btn.active {
  background: #409eff;
  color: white;
  border-color: #409eff;
}

.git-submit-form,
.manual-submit-form {
  background: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
}

.form-group .native-input,
.form-group .native-textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-sizing: border-box;
  font-family: monospace;
}

.form-group .native-textarea {
  resize: vertical;
}

.file-upload {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-input {
  padding: 8px;
  border: 2px dashed #ddd;
  border-radius: 4px;
  width: 100%;
}

.file-hint {
  font-size: 12px;
  color: #888;
}

.git-status-info {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  margin-bottom: 16px;
}

.git-status-info .status-icon {
  font-size: 20px;
}

.git-status-info .icon-success {
  color: #52c41a;
}

.git-status-info .status-text p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.git-status-info .status-text strong {
  color: #333;
}

.submit-actions {
  display: flex;
  justify-content: flex-end;
}

.primary-btn {
  padding: 10px 20px;
  background: #409eff;
  color: white;
  border: none;
  cursor: pointer;
  border-radius: 4px;
}

.primary-btn:hover {
  background: #66b1ff;
}

.warning-btn {
  padding: 10px 20px;
  background: #faad14;
  color: white;
  border: none;
  cursor: pointer;
  border-radius: 4px;
}

.warning-btn:hover {
  background: #ffc53d;
}

.submitted-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #f0fdf4;
  border-radius: 8px;
}

.submitted-status {
  display: flex;
  gap: 12px;
}

.submitted-status .status-icon {
  font-size: 24px;
  color: #52c41a;
}

.submitted-status .status-details p {
  margin: 0 0 4px;
  font-size: 14px;
  color: #666;
}

.submitted-status .status-details p:first-child {
  color: #333;
}

.evaluation-section {
  margin-top: 20px;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.eval-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.eval-badge {
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 12px;
  border-radius: 4px;
  color: white;
  font-size: 14px;
}

.eval-score {
  color: white;
  font-size: 18px;
  font-weight: bold;
}

.action-btn {
  padding: 8px 16px;
  background: white;
  color: #667eea;
  border: none;
  cursor: pointer;
  border-radius: 4px;
  font-size: 14px;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.9);
}

/* ===== Phase 2：评价状态反馈样式 ===== */
.eval-progress {
  margin-top: 12px;
  width: 100%;
}

/* Element Plus 组件内部元素在 scoped 样式中需要 :deep() 穿透 */
.evaluation-section :deep(.el-progress-bar__outer) {
  background: rgba(255, 255, 255, 0.25);
}

.evaluation-section :deep(.el-progress-bar__inner) {
  background: white;
}

/* 评价中徽标的呼吸圆点动画 */
.loading-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 6px;
  border-radius: 50%;
  background: white;
  animation: evalPulse 1s infinite ease-in-out;
}

@keyframes evalPulse {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
}

/* 失败态：红色渐变底 + 徽标加深，与完成态的紫色渐变区分 */
.evaluation-failed {
  background: linear-gradient(135deg, #f56c6c 0%, #c45656 100%);
}

.eval-badge-failed {
  background: rgba(0, 0, 0, 0.15);
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

.success-toast {
  position: fixed;
  bottom: 30px;
  right: 30px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 24px;
  background: #52c41a;
  color: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  animation: slideIn 0.3s ease;
}

.success-toast .toast-icon {
  font-size: 20px;
}

.success-toast .toast-message {
  font-size: 14px;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
</style>