<template>
  <div class="dashboard-container">
    <AppLayout
      :menu="menu"
      active-path="/teacher"
      title="教师数据看板"
      :subtitle="'欢迎回来，' + userName"
      :sidebar-gradient="SIDEBAR_GRADIENT"
      content-scroll
      @logout="logout"
    >

      <!-- 数据统计卡片 -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon blue">
            📚
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.courses }}</div>
            <div class="stat-label">课程数量</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon purple">
            📝
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.assignments }}</div>
            <div class="stat-label">作业数量</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon green">
            📤
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.submissions }}</div>
            <div class="stat-label">提交数量</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon yellow">
            ✅
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.evaluations }}</div>
            <div class="stat-label">已评价数量</div>
          </div>
        </div>
      </div>

      <!-- 最近提交 -->
      <div class="recent-section">
        <h3>最近提交</h3>
        <table class="simple-table">
          <thead>
            <tr>
              <th>学生姓名</th>
              <th>作业名称</th>
              <th>提交时间</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in recentSubmissions" :key="item.id">
              <td>{{ item.studentName }}</td>
              <td>{{ item.assignmentTitle }}</td>
              <td>{{ item.submitTime }}</td>
              <td>
                <span :class="item.status === '已评价' ? 'status-done' : 'status-pending'">
                  {{ item.status }}
                </span>
              </td>
              <td>
                <button class="action-btn" @click="$router.push('/teacher/evaluations')">
                  查看
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </AppLayout>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { courseApi, assignmentApi, submissionApi, evaluationApi, authApi } from '../../services/api'
import AppLayout from '../../components/AppLayout.vue'

const router = useRouter()

// 教师端侧边栏菜单（与原 template 硬编码一致：label + path，无 icon）
const menu = [
  { label: '数据看板', path: '/teacher' },
  { label: '课程管理', path: '/teacher/courses' },
  { label: '作业管理', path: '/teacher/assignments' },
  { label: '评价分析', path: '/teacher/evaluations' }
]

// 学生/教师端原有深色侧栏渐变（AppLayout 默认蓝色，此处按原视觉覆盖）
const SIDEBAR_GRADIENT = 'linear-gradient(180deg, #2c3e50 0%, #34495e 100%)'

const userName = computed(() => {
  const user = sessionStorage.getItem('user')
  if (user) {
    return JSON.parse(user).name
  }
  return '未知用户'
})

const stats = ref({
  courses: 0,
  assignments: 0,
  submissions: 0,
  evaluations: 0
})

const recentSubmissions = ref([])

const loadData = async () => {
  try {
    const [coursesResult, assignmentsResult] = await Promise.all([
      courseApi.getAll(),
      assignmentApi.getAll()
    ])
    
    if (coursesResult && coursesResult.success && coursesResult.data) {
      stats.value.courses = coursesResult.data.length
    }
    if (assignmentsResult && assignmentsResult.success && assignmentsResult.data) {
      stats.value.assignments = assignmentsResult.data.length
    }
    
    stats.value.submissions = 0
    stats.value.evaluations = 0
    
    recentSubmissions.value = [
      { id: 1, studentName: '暂无数据', assignmentTitle: '-', submitTime: '-', status: '-' }
    ]
  } catch (error) {
    console.error('加载教师数据失败:', error)
  }
}

onMounted(() => {
  loadData()
})

const logout = async () => {
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
</script>

<style scoped>
.dashboard-container {
  display: flex;
  min-height: 100vh;
  background: #f5f5f5;
}

/* sidebar / header 样式已迁移至 AppLayout.vue
   已逐条核对：原 .sidebar / .logo / .logo h2 / .sidebar-nav / .nav-item /
   .nav-item:hover,.active / .logout-btn / .logout-btn:hover / .main-content /
   .header h1 / .header p 与 AppLayout 现有规则完全等价，故整体删除。 */

.stats-grid {
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
  font-size: 24px;
}

.stat-icon.blue { background: #667eea; }
.stat-icon.purple { background: #f093fb; }
.stat-icon.green { background: #4ade80; }
.stat-icon.yellow { background: #fbbf24; }

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.recent-section {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.recent-section h3 {
  margin: 0 0 16px;
  color: #333;
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
}

.status-done {
  color: #52c41a;
}

.status-pending {
  color: #faad14;
}

.action-btn {
  padding: 6px 12px;
  background: #409eff;
  color: white;
  border: none;
  cursor: pointer;
  border-radius: 4px;
  font-size: 14px;
}

.action-btn:hover {
  background: #66b1ff;
}
</style>
