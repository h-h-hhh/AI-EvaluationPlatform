<template>
  <div class="assignments-container">
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
        <div class="nav-item active" @click="$router.push('/teacher/assignments')">
          作业管理
        </div>
        <div class="nav-item" @click="$router.push('/teacher/evaluations')">
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
          <h1>作业管理</h1>
          <p>管理您的作业列表</p>
        </div>
        <button class="primary-btn" @click="openCreateModal">
          + 新建作业
        </button>
      </header>
      
      <div class="filter-bar">
        <select v-model="selectedCourse" class="native-select">
          <option value="">全部课程</option>
          <option v-for="course in courses" :key="course.id" :value="course.id">
            {{ course.name }}
          </option>
        </select>
      </div>
      
      <div class="assignments-table">
        <table class="simple-table">
          <thead>
            <tr>
              <th>作业标题</th>
              <th>所属课程</th>
              <th>总分</th>
              <th>截止日期</th>
              <th>Git配置</th>
              <th>提交人数</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filteredAssignments" :key="item.id">
              <td>{{ item.title }}</td>
              <td>{{ item.courseName }}</td>
              <td>{{ item.totalScore }}</td>
              <td>{{ item.deadline }}</td>
              <td>
                <span v-if="item.gitEnabled" class="git-status enabled" @click="openGitConfig(item)">
                  已配置
                </span>
                <span v-else class="git-status disabled" @click="openGitConfig(item)">
                  未配置
                </span>
              </td>
              <td>{{ item.submissionCount }}</td>
              <td>
                <span :class="item.status === '进行中' ? 'status-success' : 'status-info'">
                  {{ item.status }}
                </span>
              </td>
              <td>
                <button class="action-btn" @click="viewSubmissions(item)">提交</button>
                <button class="action-btn" @click="openEvaluationConfig(item)">评价规则</button>
                <button class="action-btn" @click="openEditModal(item)">编辑</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <!-- 创建/编辑作业模态框 -->
      <div v-if="showCreateModal" class="modal-overlay">
        <div class="modal-content wide">
          <h3>{{ isEditing ? '编辑作业' : '新建作业' }}</h3>
          <div class="form-row">
            <div class="form-group">
              <label>作业标题 *</label>
              <input v-model="assignmentForm.title" placeholder="请输入作业标题" class="native-input" />
            </div>
            <div class="form-group">
              <label>所属课程 *</label>
              <select v-model="assignmentForm.courseId" class="native-select">
                <option value="">请选择课程</option>
                <option v-for="course in courses" :key="course.id" :value="course.id">
                  {{ course.name }}
                </option>
              </select>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>总分</label>
              <input v-model="assignmentForm.totalScore" type="number" placeholder="100" class="native-input" />
            </div>
            <div class="form-group">
              <label>截止日期</label>
              <input v-model="assignmentForm.deadline" type="datetime-local" class="native-input" />
            </div>
          </div>
          <div class="form-group">
            <label>作业描述</label>
            <textarea v-model="assignmentForm.description" placeholder="请输入作业描述" rows="3" class="native-textarea"></textarea>
          </div>
          
          <!-- Git仓库配置 -->
          <div class="section-divider">
            <h4>Git 仓库配置</h4>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>仓库类型</label>
              <select v-model="assignmentForm.gitConfig.repoType" class="native-select">
                <option value="github">GitHub</option>
                <option value="gitlab">GitLab</option>
                <option value="gitee">Gitee</option>
                <option value="custom">自定义</option>
              </select>
            </div>
            <div class="form-group">
              <label>仓库URL</label>
              <input v-model="assignmentForm.gitConfig.repoUrl" placeholder="https://github.com/xxx/repo" class="native-input" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>分支名称</label>
              <input v-model="assignmentForm.gitConfig.branch" placeholder="main 或 master" class="native-input" />
            </div>
            <div class="form-group">
              <label>作业目录</label>
              <input v-model="assignmentForm.gitConfig.workDir" placeholder="如: src/homework1" class="native-input" />
            </div>
          </div>
          <div class="form-group checkbox-group">
            <label class="checkbox-label">
              <input type="checkbox" v-model="assignmentForm.gitConfig.enabled" />
              <span>启用自动代码采集</span>
            </label>
          </div>
          
          <!-- 评价规则配置 -->
          <div class="section-divider">
            <h4>评价规则配置</h4>
          </div>
          <div class="evaluation-rules">
            <div class="rule-item">
              <div class="rule-header">
                <span class="rule-name">正确性评分</span>
                <span class="rule-weight">{{ assignmentForm.evaluationRules.correctnessWeight }}%</span>
              </div>
              <input type="range" v-model="assignmentForm.evaluationRules.correctnessWeight" min="0" max="100" class="weight-slider" />
              <p class="rule-desc">基于自动化测试用例通过率</p>
            </div>
            <div class="rule-item">
              <div class="rule-header">
                <span class="rule-name">代码质量</span>
                <span class="rule-weight">{{ assignmentForm.evaluationRules.qualityWeight }}%</span>
              </div>
              <input type="range" v-model="assignmentForm.evaluationRules.qualityWeight" min="0" max="100" class="weight-slider" />
              <p class="rule-desc">集成 SonarQube 静态分析</p>
            </div>
            <div class="rule-item">
              <div class="rule-header">
                <span class="rule-name">原创性评估</span>
                <span class="rule-weight">{{ assignmentForm.evaluationRules.originalityWeight }}%</span>
              </div>
              <input type="range" v-model="assignmentForm.evaluationRules.originalityWeight" min="0" max="100" class="weight-slider" />
              <p class="rule-desc">代码相似度检测与AI贡献分析</p>
            </div>
            <div class="rule-item">
              <div class="rule-header">
                <span class="rule-name">过程性评分</span>
                <span class="rule-weight">{{ assignmentForm.evaluationRules.processWeight }}%</span>
              </div>
              <input type="range" v-model="assignmentForm.evaluationRules.processWeight" min="0" max="100" class="weight-slider" />
              <p class="rule-desc">提交频率、代码增量、分支管理</p>
            </div>
            <div class="rule-item">
              <div class="rule-header">
                <span class="rule-name">LLM辅助分析</span>
                <label class="switch">
                  <input type="checkbox" v-model="assignmentForm.evaluationRules.llmEnabled" />
                  <span class="slider"></span>
                </label>
              </div>
              <p class="rule-desc">启用LLM智能代码分析（需配置API密钥）</p>
            </div>
          </div>
          
          <div class="modal-actions">
            <button class="secondary-btn" @click="closeModal">取消</button>
            <button class="primary-btn" @click="saveAssignment">{{ isEditing ? '保存' : '创建' }}</button>
          </div>
        </div>
      </div>
      
      <!-- Git配置详情模态框 -->
      <div v-if="showGitModal" class="modal-overlay">
        <div class="modal-content">
          <h3>Git 仓库配置</h3>
          <div class="config-detail">
            <div class="config-item">
              <label>仓库类型</label>
              <span>{{ currentAssignment.gitConfig?.repoType || '未配置' }}</span>
            </div>
            <div class="config-item">
              <label>仓库URL</label>
              <code>{{ currentAssignment.gitConfig?.repoUrl || '未配置' }}</code>
            </div>
            <div class="config-item">
              <label>分支</label>
              <span>{{ currentAssignment.gitConfig?.branch || 'main' }}</span>
            </div>
            <div class="config-item">
              <label>作业目录</label>
              <span>{{ currentAssignment.gitConfig?.workDir || '/' }}</span>
            </div>
            <div class="config-item">
              <label>状态</label>
              <span :class="currentAssignment.gitConfig?.enabled ? 'status-success' : 'status-warning'">
                {{ currentAssignment.gitConfig?.enabled ? '已启用' : '未启用' }}
              </span>
            </div>
          </div>
          <div class="modal-actions">
            <button class="secondary-btn" @click="showGitModal = false">关闭</button>
            <button class="primary-btn" @click="testGitConnection">测试连接</button>
          </div>
        </div>
      </div>
      
      <!-- 评价规则详情模态框 -->
      <div v-if="showEvalModal" class="modal-overlay">
        <div class="modal-content">
          <h3>评价规则配置</h3>
          <div class="evaluation-summary">
            <div class="summary-chart">
              <div class="chart-bars">
                <div class="bar-item">
                  <div class="bar" :style="{ height: currentAssignment.evaluationRules?.correctnessWeight + '%' }"></div>
                  <span>正确性 {{ currentAssignment.evaluationRules?.correctnessWeight }}%</span>
                </div>
                <div class="bar-item">
                  <div class="bar quality" :style="{ height: currentAssignment.evaluationRules?.qualityWeight + '%' }"></div>
                  <span>代码质量 {{ currentAssignment.evaluationRules?.qualityWeight }}%</span>
                </div>
                <div class="bar-item">
                  <div class="bar originality" :style="{ height: currentAssignment.evaluationRules?.originalityWeight + '%' }"></div>
                  <span>原创性 {{ currentAssignment.evaluationRules?.originalityWeight }}%</span>
                </div>
                <div class="bar-item">
                  <div class="bar process" :style="{ height: currentAssignment.evaluationRules?.processWeight + '%' }"></div>
                  <span>过程性 {{ currentAssignment.evaluationRules?.processWeight }}%</span>
                </div>
              </div>
            </div>
            <div class="summary-list">
              <div class="summary-item">
                <span class="summary-label">LLM智能分析</span>
                <span :class="currentAssignment.evaluationRules?.llmEnabled ? 'status-success' : 'status-warning'">
                  {{ currentAssignment.evaluationRules?.llmEnabled ? '已启用' : '未启用' }}
                </span>
              </div>
              <div class="summary-item">
                <span class="summary-label">SonarQube分析</span>
                <span class="status-success">已集成</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">总分</span>
                <span class="total-score">{{ currentAssignment.totalScore }}分</span>
              </div>
            </div>
          </div>
          <div class="modal-actions">
            <button class="secondary-btn" @click="showEvalModal = false">关闭</button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { assignmentApi, courseApi } from '../../services/api'

const router = useRouter()

const showCreateModal = ref(false)
const showGitModal = ref(false)
const showEvalModal = ref(false)
const isEditing = ref(false)
const selectedCourse = ref('')
const currentAssignment = ref({})

const courses = ref([])
const assignments = ref([])

const loadCourses = async () => {
  try {
    const response = await courseApi.getAll()
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
        totalScore: a.totalScore,
        deadline: a.deadline ? a.deadline.replace('T', ' ') : '',
        submissionCount: 0,
        status: a.active ? '进行中' : '已截止',
        description: a.description,
        gitEnabled: false,
        gitConfig: {},
        evaluationRules: {}
      }))
    }
  } catch (error) {
    console.error('加载作业失败:', error)
  }
}

onMounted(() => {
  loadCourses()
  loadAssignments()
})

const defaultGitConfig = {
  repoType: 'github',
  repoUrl: '',
  branch: 'main',
  workDir: '',
  enabled: false
}

const defaultEvaluationRules = {
  correctnessWeight: 40,
  qualityWeight: 25,
  originalityWeight: 20,
  processWeight: 15,
  llmEnabled: false
}

const assignmentForm = reactive({
  id: null,
  title: '',
  courseId: '',
  totalScore: 100,
  deadline: '',
  description: '',
  gitConfig: { ...defaultGitConfig },
  evaluationRules: { ...defaultEvaluationRules }
})

const filteredAssignments = computed(() => {
  if (!selectedCourse.value) return assignments.value
  return assignments.value.filter(a => a.courseId === parseInt(selectedCourse.value))
})

const handleLogout = () => {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('role')
  sessionStorage.removeItem('user')
  router.push('/')
}

const openCreateModal = () => {
  isEditing.value = false
  Object.assign(assignmentForm, {
    id: null,
    title: '',
    courseId: '',
    totalScore: 100,
    deadline: '',
    description: '',
    gitConfig: { ...defaultGitConfig },
    evaluationRules: { ...defaultEvaluationRules }
  })
  showCreateModal.value = true
}

const openEditModal = (item) => {
  isEditing.value = true
  Object.assign(assignmentForm, JSON.parse(JSON.stringify(item)))
  showCreateModal.value = true
}

const closeModal = () => {
  showCreateModal.value = false
  showGitModal.value = false
  showEvalModal.value = false
}

const saveAssignment = async () => {
  if (!assignmentForm.title || !assignmentForm.courseId) {
    alert('请填写必填字段')
    return
  }
  
  const course = courses.value.find(c => c.id === parseInt(assignmentForm.courseId))
  
  try {
    const data = {
      title: assignmentForm.title,
      courseId: parseInt(assignmentForm.courseId),
      totalScore: assignmentForm.totalScore,
      deadline: assignmentForm.deadline,
      description: assignmentForm.description,
      gradingRules: JSON.stringify(assignmentForm.evaluationRules),
      testCases: JSON.stringify(assignmentForm.gitConfig)
    }
    
    if (isEditing.value) {
      await assignmentApi.update(assignmentForm.id, data)
      alert('作业更新成功')
    } else {
      await assignmentApi.create(data)
      alert('作业创建成功')
    }
    closeModal()
    loadAssignments()
  } catch (error) {
    console.error('保存作业失败:', error)
    alert('保存作业失败，请重试')
  }
}

const viewSubmissions = (assignment) => {
  alert(`查看作业 "${assignment.title}" 的提交列表\n提交人数: ${assignment.submissionCount}\nGit配置: ${assignment.gitEnabled ? '已启用' : '未启用'}`)
}

const openGitConfig = (item) => {
  currentAssignment.value = item
  showGitModal.value = true
}

const openEvaluationConfig = (item) => {
  currentAssignment.value = item
  showEvalModal.value = true
}

const testGitConnection = () => {
  alert('正在测试Git仓库连接...\n连接成功！')
}
</script>

<style scoped>
.assignments-container {
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

.filter-bar {
  margin-bottom: 20px;
}

.native-select {
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  min-width: 200px;
}

.assignments-table {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
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

.simple-table td {
  color: #333;
  font-size: 14px;
}

.git-status {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.git-status.enabled {
  background: #dcfce7;
  color: #16a34a;
}

.git-status.disabled {
  background: #fef3c7;
  color: #d97706;
}

.status-success { color: #52c41a; }
.status-info { color: #1890ff; }
.status-warning { color: #faad14; }

.action-btn {
  padding: 6px 12px;
  margin-right: 8px;
  border: 1px solid #ddd;
  background: white;
  cursor: pointer;
  border-radius: 4px;
  font-size: 12px;
}

.action-btn:hover {
  background: #f5f5f5;
}

.action-btn.danger {
  color: #ff4d4f;
  border-color: #ff4d4f;
}

/* Modal styles */
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

.modal-content {
  background: white;
  padding: 24px;
  border-radius: 12px;
  width: 500px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-content.wide {
  width: 700px;
}

.modal-content h3 {
  margin: 0 0 20px;
  color: #333;
  font-size: 18px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
  font-size: 14px;
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-row .form-group {
  flex: 1;
}

.native-input,
.native-textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-sizing: border-box;
  font-size: 14px;
}

.native-textarea {
  resize: vertical;
}

.checkbox-group {
  margin-top: 12px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.checkbox-label input {
  margin-right: 8px;
}

.section-divider {
  margin: 24px 0 16px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.section-divider h4 {
  margin: 0;
  color: #333;
  font-size: 16px;
}

.evaluation-rules {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.rule-item {
  background: #f9f9f9;
  padding: 16px;
  border-radius: 8px;
}

.rule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.rule-name {
  font-weight: 500;
  color: #333;
}

.rule-weight {
  font-weight: bold;
  color: #667eea;
}

.weight-slider {
  width: 100%;
  margin: 8px 0;
}

.rule-desc {
  margin: 0;
  font-size: 12px;
  color: #888;
}

/* Switch */
.switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 24px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .slider {
  background-color: #667eea;
}

input:checked + .slider:before {
  transform: translateX(20px);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #eee;
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

.secondary-btn {
  padding: 10px 20px;
  background: white;
  color: #666;
  border: 1px solid #ddd;
  cursor: pointer;
  border-radius: 4px;
}

.secondary-btn:hover {
  background: #f5f5f5;
}

/* Config detail */
.config-detail {
  display: grid;
  gap: 16px;
}

.config-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.config-item label {
  font-size: 12px;
  color: #888;
}

.config-item code {
  background: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
  font-size: 13px;
  word-break: break-all;
}

/* Evaluation summary */
.evaluation-summary {
  display: flex;
  gap: 24px;
}

.summary-chart {
  flex: 1;
}

.chart-bars {
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  height: 200px;
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.bar-item .bar {
  width: 40px;
  background: #667eea;
  border-radius: 4px 4px 0 0;
  transition: height 0.3s;
}

.bar-item .bar.quality { background: #f093fb; }
.bar-item .bar.originality { background: #4ade80; }
.bar-item .bar.process { background: #fbbf24; }

.bar-item span {
  font-size: 11px;
  color: #666;
  text-align: center;
}

.summary-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
}

.summary-label {
  color: #666;
}

.total-score {
  font-size: 20px;
  font-weight: bold;
  color: #667eea;
}
</style>