<template>
  <div class="dashboard-container">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="logo">
        <h2>代码评价平台</h2>
      </div>
      <nav class="sidebar-nav">
        <div class="nav-item active" @click="$router.push('/student')">
          学习看板
        </div>
        <div class="nav-item" @click="$router.push('/student/courses')">
          我的课程
        </div>
        <div class="nav-item" @click="$router.push('/student/submissions')">
          作业提交
        </div>
        <div class="nav-item" @click="$router.push('/student/results')">
          评价结果
        </div>
      </nav>
      <div class="logout-btn" @click="logout">
        退出登录
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <header class="header">
        <h1>学生学习看板</h1>
        <p>欢迎回来，{{ userName }}</p>
      </header>

      <!-- 数据统计卡片 -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon blue">
            📚
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.courses }}</div>
            <div class="stat-label">已选课程</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon purple">
            📝
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.assignments }}</div>
            <div class="stat-label">待完成作业</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon green">
            📤
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.submitted }}</div>
            <div class="stat-label">已提交作业</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon yellow">
            ✅
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.evaluated }}</div>
            <div class="stat-label">已评价作业</div>
          </div>
        </div>
      </div>

      <!-- 最近作业 -->
      <div class="recent-section">
        <h3>最近作业</h3>
        <table class="simple-table">
          <thead>
            <tr>
              <th>作业名称</th>
              <th>所属课程</th>
              <th>截止时间</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in recentAssignments" :key="item.id">
              <td>{{ item.title }}</td>
              <td>{{ item.course }}</td>
              <td>{{ item.deadline }}</td>
              <td>
                <span :class="item.status === '已提交' ? 'status-done' : 'status-pending'">
                  {{ item.status }}
                </span>
              </td>
              <td>
                <button class="action-btn" @click="$router.push('/student/submissions')">
                  {{ item.status === '已提交' ? '查看' : '提交' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { courseApi, assignmentApi, authApi } from '../../services/api'

const router = useRouter()

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
  submitted: 0,
  evaluated: 0
})

const recentAssignments = ref([])

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
      recentAssignments.value = assignmentsResult.data.slice(0, 4).map(a => ({
        id: a.id,
        title: a.title,
        course: a.course?.name || '',
        deadline: a.deadline ? a.deadline.replace('T', ' ') : '',
        status: '待提交'
      }))
    }
  } catch (error) {
    console.error('加载学生数据失败:', error)
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

.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #2c3e50 0%, #34495e 100%);
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
  overflow: auto;
}

.header h1 {
  margin: 0 0 5px;
  color: #333;
}

.header p {
  color: #666;
  margin: 0 0 20px;
}

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