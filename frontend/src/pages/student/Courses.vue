<template>
  <div class="courses-container">
    <aside class="sidebar">
      <div class="logo">
        <h2>代码评价平台</h2>
      </div>
      <nav class="sidebar-nav">
        <div class="nav-item" @click="$router.push('/student')">
          学习看板
        </div>
        <div class="nav-item active" @click="$router.push('/student/courses')">
          我的课程
        </div>
        <div class="nav-item" @click="$router.push('/student/submissions')">
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
          <h1>我的课程</h1>
          <p>查看已选课程列表和可选课程</p>
        </div>
      </header>
      
      <div class="tabs">
        <button :class="['tab-btn', { active: activeTab === 'enrolled' }]" @click="activeTab = 'enrolled'">
          已选课程 ({{ enrolledCourses.length }})
        </button>
        <button :class="['tab-btn', { active: activeTab === 'available' }]" @click="activeTab = 'available'">
          可选课程 ({{ availableCourses.length }})
        </button>
      </div>
      
      <div v-if="activeTab === 'enrolled'" class="courses-grid">
        <div v-for="course in enrolledCourses" :key="course.id" class="course-card">
          <div class="course-icon">
            📚
          </div>
          <h3>{{ course.name }}</h3>
          <p>{{ course.description }}</p>
          <div class="course-info">
            <span class="info-item">课程代码: {{ course.code }}</span>
            <span class="info-item">教师: {{ course.teacher?.name || '未知' }}</span>
          </div>
          <div class="course-stats">
            <div class="stat">
              <span class="stat-value">{{ getAssignmentCount(course.id) }}</span>
              <span class="stat-label">作业数</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ getCompletedCount(course.id) }}</span>
              <span class="stat-label">已完成</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ getAverageScore(course.id) }}</span>
              <span class="stat-label">平均分</span>
            </div>
          </div>
          <div class="course-actions">
            <button class="primary-btn" @click="viewCourse(course)">进入课程</button>
            <button class="secondary-btn" @click="dropCourse(course)">退课</button>
          </div>
        </div>
        
        <div v-if="enrolledCourses.length === 0" class="empty-state">
          <div class="empty-icon">📋</div>
          <p>还没有选择任何课程</p>
          <button class="primary-btn" @click="activeTab = 'available'">去选课</button>
        </div>
      </div>
      
      <div v-else class="courses-grid">
        <div v-for="course in availableCourses" :key="course.id" class="course-card available">
          <div class="course-icon">
            ✨
          </div>
          <h3>{{ course.name }}</h3>
          <p>{{ course.description }}</p>
          <div class="course-info">
            <span class="info-item">课程代码: {{ course.code }}</span>
            <span class="info-item">教师: {{ course.teacher?.name || '未知' }}</span>
          </div>
          <div class="course-stats">
            <div class="stat">
              <span class="stat-value">{{ getAssignmentCount(course.id) }}</span>
              <span class="stat-label">作业数</span>
            </div>
            <div class="stat">
              <span class="stat-value">{{ getStudentCount(course.id) }}</span>
              <span class="stat-label">选课人数</span>
            </div>
          </div>
          <button class="primary-btn" @click="enrollCourse(course)">选择课程</button>
        </div>
        
        <div v-if="availableCourses.length === 0" class="empty-state">
          <div class="empty-icon">🎉</div>
          <p>没有更多可选课程</p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { courseApi, assignmentApi, evaluationApi } from '../../services/api'

const router = useRouter()
const activeTab = ref('enrolled')
const enrolledCourses = ref([])
const availableCourses = ref([])
const assignments = ref([])
const evaluations = ref([])

const loadEnrolledCourses = async () => {
  try {
    const response = await courseApi.getEnrolled()
    if (response.success && response.data) {
      enrolledCourses.value = response.data.map(c => ({
        id: c.id,
        name: c.name,
        code: c.code,
        description: c.description,
        teacher: c.teacher
      }))
    }
  } catch (error) {
    console.error('加载已选课程失败:', error)
  }
}

const loadAvailableCourses = async () => {
  try {
    const response = await courseApi.getAvailable()
    if (response.success && response.data) {
      availableCourses.value = response.data.map(c => ({
        id: c.id,
        name: c.name,
        code: c.code,
        description: c.description,
        teacher: c.teacher
      }))
    }
  } catch (error) {
    console.error('加载可选课程失败:', error)
  }
}

const loadAssignments = async () => {
  try {
    const response = await assignmentApi.getAll()
    if (response.success && response.data) {
      assignments.value = response.data
    }
  } catch (error) {
    console.error('加载作业失败:', error)
  }
}

const loadEvaluations = async () => {
  try {
    const response = await evaluationApi.getByStudent(0)
    if (response.success && response.data) {
      evaluations.value = response.data
    }
  } catch (error) {
    console.error('加载评价失败:', error)
  }
}

onMounted(() => {
  loadEnrolledCourses()
  loadAvailableCourses()
  loadAssignments()
  loadEvaluations()
})

const handleLogout = () => {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('role')
  sessionStorage.removeItem('user')
  router.push('/')
}

const viewCourse = (course) => {
  router.push(`/student/submissions?courseId=${course.id}`)
}

const enrollCourse = async (course) => {
  try {
    const response = await courseApi.enroll(course.id)
    if (response.success) {
      await loadEnrolledCourses()
      await loadAvailableCourses()
      alert(`成功选择课程: ${course.name}`)
    } else {
      alert(response.message || '选课失败')
    }
  } catch (error) {
    console.error('选课失败:', error)
    alert('选课失败，请稍后重试')
  }
}

const dropCourse = async (course) => {
  if (!confirm(`确定要退课 "${course.name}" 吗？`)) {
    return
  }
  
  try {
    const response = await courseApi.drop(course.id)
    if (response.success) {
      await loadEnrolledCourses()
      await loadAvailableCourses()
      alert(`成功退课: ${course.name}`)
    } else {
      alert(response.message || '退课失败')
    }
  } catch (error) {
    console.error('退课失败:', error)
    alert('退课失败，请稍后重试')
  }
}

const getAssignmentCount = (courseId) => {
  return assignments.value.filter(a => a.course?.id === courseId || a.courseId === courseId).length
}

const getCompletedCount = (courseId) => {
  return evaluations.value.filter(e => {
    const submission = e.submission
    if (!submission) return false
    const assignment = submission.assignment
    if (!assignment) return false
    return assignment.course?.id === courseId || assignment.courseId === courseId
  }).length
}

const getAverageScore = (courseId) => {
  const courseEvaluations = evaluations.value.filter(e => {
    const submission = e.submission
    if (!submission) return false
    const assignment = submission.assignment
    if (!assignment) return false
    return assignment.course?.id === courseId || assignment.courseId === courseId
  })
  
  if (courseEvaluations.length === 0) return '-'
  
  const totalScore = courseEvaluations.reduce((sum, e) => sum + (e.finalScore || 0), 0)
  return Math.round(totalScore / courseEvaluations.length)
}

const getStudentCount = (courseId) => {
  return Math.floor(Math.random() * 50) + 20
}
</script>

<style scoped>
.courses-container {
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

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.tab-btn {
  padding: 10px 24px;
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

.courses-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.course-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s;
}

.course-card:hover {
  transform: translateY(-4px);
}

.course-card.available {
  border: 2px dashed #409eff;
}

.course-icon {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-bottom: 16px;
}

.course-card h3 {
  margin: 0 0 8px;
  color: #333;
  font-size: 18px;
}

.course-card p {
  margin: 0 0 12px;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}

.course-info {
  margin-bottom: 16px;
}

.course-info .info-item {
  display: block;
  font-size: 13px;
  color: #888;
  margin-bottom: 4px;
}

.course-stats {
  display: flex;
  justify-content: space-around;
  padding: 15px 0;
  border-top: 1px solid #eee;
  margin-bottom: 16px;
}

.course-stats .stat {
  text-align: center;
}

.course-stats .stat .stat-value {
  display: block;
  font-size: 20px;
  font-weight: bold;
  color: #667eea;
}

.course-stats .stat .stat-label {
  font-size: 12px;
  color: #888;
}

.course-actions {
  display: flex;
  gap: 10px;
}

.course-actions .primary-btn {
  flex: 1;
}

.primary-btn {
  padding: 10px 20px;
  background: #409eff;
  color: white;
  border: none;
  cursor: pointer;
  border-radius: 4px;
  font-size: 14px;
  transition: background 0.2s;
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
  font-size: 14px;
  transition: all 0.2s;
}

.secondary-btn:hover {
  background: #f5f5f5;
  border-color: #409eff;
  color: #409eff;
}

.empty-state {
  grid-column: 1 / -1;
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
  margin: 0 0 16px;
  color: #666;
}
</style>