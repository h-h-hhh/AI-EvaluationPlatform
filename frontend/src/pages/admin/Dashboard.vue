<template>
  <div class="dashboard-container">
    <aside class="sidebar">
      <div class="logo">
        <h2>代码评价平台</h2>
      </div>
      <nav class="sidebar-nav">
        <div class="nav-item active" @click="$router.push('/admin')">
          系统看板
        </div>
        <div class="nav-item" @click="$router.push('/admin/users')">
          用户管理
        </div>
        <div class="nav-item" @click="$router.push('/admin/courses')">
          课程管理
        </div>
        <div class="nav-item" @click="$router.push('/admin/statistics')">
          数据统计
        </div>
      </nav>
      <div class="logout-btn" @click="logout">
        退出登录
      </div>
    </aside>

    <main class="main-content">
      <header class="header">
        <h1>系统管理看板</h1>
        <p>欢迎回来，{{ userName }}</p>
      </header>

      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon blue">
            👥
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalUsers }}</div>
            <div class="stat-label">总用户数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon purple">
            👨‍🏫
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.teachers }}</div>
            <div class="stat-label">教师数量</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon green">
            🧑‍🎓
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.students }}</div>
            <div class="stat-label">学生数量</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon orange">
            📚
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.courses }}</div>
            <div class="stat-label">课程数量</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon pink">
            📝
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.assignments }}</div>
            <div class="stat-label">作业数量</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon yellow">
            📤
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.submissions }}</div>
            <div class="stat-label">提交数量</div>
          </div>
        </div>
      </div>

      <div class="recent-section">
        <h3>最近用户注册</h3>
        <table class="simple-table">
          <thead>
            <tr>
              <th>用户名</th>
              <th>姓名</th>
              <th>角色</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in recentUsers" :key="user.id">
              <td>{{ user.username }}</td>
              <td>{{ user.name }}</td>
              <td>
                <span :class="getRoleClass(user.role)">
                  {{ getRoleText(user.role) }}
                </span>
              </td>
              <td>{{ user.createdAt }}</td>
              <td>
                <button class="action-btn" @click="editUser(user)">编辑</button>
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
import { statisticsApi, authApi } from '../../services/api'

const router = useRouter()

const userName = computed(() => {
  const user = sessionStorage.getItem('user')
  if (user) {
    return JSON.parse(user).name
  }
  return '未知用户'
})

const stats = ref({
  totalUsers: 0,
  teachers: 0,
  students: 0,
  courses: 0,
  assignments: 0,
  submissions: 0,
  evaluations: 0,
  avgScore: 0
})

const recentUsers = ref([])

const loadData = async () => {
  try {
    const [overviewResult, usersResult] = await Promise.all([
      statisticsApi.getOverview(),
      statisticsApi.getRecentUsers()
    ])
    
    if (overviewResult && overviewResult.success && overviewResult.data) {
      stats.value = overviewResult.data
    }
    
    if (usersResult && usersResult.success && usersResult.data) {
      recentUsers.value = usersResult.data.slice(0, 4)
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

onMounted(() => {
  loadData()
})

const getRoleText = (role) => {
  const map = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }
  return map[role] || role
}

const getRoleClass = (role) => {
  const map = { ADMIN: 'role-admin', TEACHER: 'role-teacher', STUDENT: 'role-student' }
  return map[role] || ''
}

const editUser = (user) => {
  router.push({ path: '/admin/users', query: { edit: user.id } })
}

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
  background: linear-gradient(180deg, #8b5cf6 0%, #6d28d9 100%);
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
  grid-template-columns: repeat(6, 1fr);
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
.stat-icon.purple { background: #a855f7; }
.stat-icon.green { background: #4ade80; }
.stat-icon.orange { background: #fb923c; }
.stat-icon.pink { background: #f472b6; }
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

.role-admin {
  color: #8b5cf6;
  padding: 4px 8px;
  background: #ede9fe;
  border-radius: 4px;
  font-size: 12px;
}

.role-teacher {
  color: #3b82f6;
  padding: 4px 8px;
  background: #dbeafe;
  border-radius: 4px;
  font-size: 12px;
}

.role-student {
  color: #10b981;
  padding: 4px 8px;
  background: #d1fae5;
  border-radius: 4px;
  font-size: 12px;
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
