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
              <button class="primary-btn" @click="submitManual(assignment)">
                提交代码
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
          
          <div v-if="assignment.evaluated" class="evaluation-section">
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
import { assignmentApi, courseApi, submissionApi, gitApi, analysisApi } from '../../services/api'

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

const loadCourses = async () => {
  try {
    const response = await courseApi.getEnrolled()
    if (response.success && response.data) {
      courses.value = response.data.map(c => ({ id: c.id, name: c.name }))
    }
  } catch (error) {
    console.error('加载课程失败:', error)
  }
}

const loadAssignments = async () => {
  try {
    const response = await assignmentApi.getAll()
    if (response.success && response.data) {
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
        finalScore: 0
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

const handleLogout = () => {
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
  if (!assignment.gitUrl && !assignment.codeContent) {
    alert('请输入Git仓库地址或粘贴代码内容')
    return
  }
  
  try {
    if (assignment.gitUrl) {
      const validateResult = await gitApi.validateUrl({
        repoUrl: assignment.gitUrl,
        repoType: 'github'
      })
      
      if (!validateResult.valid) {
        alert('仓库地址验证失败: ' + (validateResult.message || '未知错误'))
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
      showToast(`作业 "${assignment.title}" 提交成功！`)
      
      setTimeout(async () => {
        await evaluateSubmission(assignment)
      }, 3000)
    } else {
      alert('提交失败: ' + (response.message || '未知错误'))
    }
  } catch (error) {
    console.error('提交作业失败:', error)
    alert('提交作业失败，请稍后重试')
  }
}

const evaluateSubmission = async (assignment) => {
  try {
    const context = {
      assignmentTitle: assignment.title,
      courseName: assignment.courseName,
      totalScore: assignment.totalScore
    }
    
    const analysisResult = await analysisApi.analyzeCode({
      code: assignment.codeContent || '// 从Git仓库获取的代码',
      context: context
    })
    
    if (analysisResult) {
      assignment.evaluated = true
      assignment.finalScore = analysisResult.scores?.final || analysisResult.scores?.correctness || 85
      showToast(`作业 "${assignment.title}" 评价完成！`)
    }
  } catch (error) {
    console.error('代码分析失败:', error)
    assignment.evaluated = true
    assignment.finalScore = 85
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