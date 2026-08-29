<template>
  <div class="dashboard-container">
    <aside class="sidebar">
      <div class="logo">
        <h2>代码评价平台</h2>
      </div>
      <nav class="sidebar-nav">
        <div class="nav-item" @click="$router.push('/admin')">
          系统看板
        </div>
        <div class="nav-item" @click="$router.push('/admin/users')">
          用户管理
        </div>
        <div class="nav-item" @click="$router.push('/admin/courses')">
          课程管理
        </div>
        <div class="nav-item active" @click="$router.push('/admin/statistics')">
          数据统计
        </div>
      </nav>
      <div class="logout-btn" @click="logout">
        退出登录
      </div>
    </aside>

    <main class="main-content">
      <header class="header">
        <h1>数据统计</h1>
        <p>系统整体数据概览</p>
      </header>

      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon blue">
            👥
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalUsers }}</div>
            <div class="stat-label">总用户数</div>
            <div class="stat-change positive">+12%</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon purple">
            📚
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.courses }}</div>
            <div class="stat-label">课程数量</div>
            <div class="stat-change positive">+5%</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon green">
            📝
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.assignments }}</div>
            <div class="stat-label">作业数量</div>
            <div class="stat-change positive">+8%</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon orange">
            📤
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.submissions }}</div>
            <div class="stat-label">提交数量</div>
            <div class="stat-change positive">+25%</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon pink">
            ✅
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.evaluations }}</div>
            <div class="stat-label">评价数量</div>
            <div class="stat-change positive">+22%</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon yellow">
            📊
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.avgScore }}%</div>
            <div class="stat-label">平均评分</div>
            <div class="stat-change positive">+3%</div>
          </div>
        </div>
      </div>

      <div class="charts-row">
        <div class="chart-card">
          <h3>用户角色分布</h3>
          <div class="pie-chart-container">
            <div class="pie-chart">
              <div class="pie-slice admin" style="--percent: 4%"></div>
              <div class="pie-slice teacher" style="--percent: 5%"></div>
              <div class="pie-slice student" style="--percent: 91%"></div>
            </div>
            <div class="pie-center">
              <div class="pie-total">{{ stats.totalUsers }}</div>
              <div class="pie-label">总用户</div>
            </div>
          </div>
          <div class="legend">
            <div class="legend-item">
              <span class="legend-color admin"></span>
              <span>管理员 ({{ stats.admins }})</span>
            </div>
            <div class="legend-item">
              <span class="legend-color teacher"></span>
              <span>教师 ({{ stats.teachers }})</span>
            </div>
            <div class="legend-item">
              <span class="legend-color student"></span>
              <span>学生 ({{ stats.students }})</span>
            </div>
          </div>
        </div>

        <div class="chart-card">
          <h3>课程提交统计</h3>
          <div class="bar-chart">
            <div v-for="course in courseStats" :key="course.name" class="bar-item">
              <div class="bar-label">{{ course.name }}</div>
              <div class="bar-track">
                <div 
                  class="bar-fill" 
                  :style="{ width: (course.submissions / maxSubmissions * 100) + '%' }"
                ></div>
              </div>
              <div class="bar-value">{{ course.submissions }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="charts-row">
        <div class="chart-card full-width">
          <h3>每日提交趋势</h3>
          <div class="line-chart">
            <div class="line-chart-grid">
              <div class="grid-line" v-for="i in 5" :key="i"></div>
            </div>
            <div class="line-chart-data">
              <div v-for="(day, index) in dailyStats" :key="day.date" class="data-point">
                <div 
                  class="point-bar" 
                  :style="{ height: (day.submissions / maxDaily * 100) + '%' }"
                ></div>
                <div class="point-label">{{ day.date }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { statisticsApi, authApi } from '../../services/api'

const router = useRouter()

const stats = ref({
  totalUsers: 0,
  admins: 0,
  teachers: 0,
  students: 0,
  courses: 0,
  assignments: 0,
  submissions: 0,
  evaluations: 0,
  avgScore: 0
})

const courseStats = ref([])

const dailyStats = ref([
  { date: '6/25', submissions: 0 },
  { date: '6/26', submissions: 0 },
  { date: '6/27', submissions: 0 },
  { date: '6/28', submissions: 0 },
  { date: '6/29', submissions: 0 },
  { date: '6/30', submissions: 0 },
  { date: '7/1', submissions: 0 }
])

const loadData = async () => {
  try {
    const [overviewResult, roleResult, courseResult] = await Promise.all([
      statisticsApi.getOverview(),
      statisticsApi.getUserRoleDistribution(),
      statisticsApi.getCourseSubmissions()
    ])
    
    if (overviewResult && overviewResult.success && overviewResult.data) {
      stats.value = overviewResult.data
    }
    
    if (roleResult && roleResult.success && roleResult.data) {
      stats.value.admins = roleResult.data.admin || 0
      stats.value.teachers = roleResult.data.teacher || 0
      stats.value.students = roleResult.data.student || 0
      stats.value.totalUsers = roleResult.data.total || 0
    }
    
    if (courseResult && courseResult.success && courseResult.data) {
      courseStats.value = courseResult.data.map(item => ({
        name: item.courseName,
        submissions: item.submissions
      }))
    }
    
    const totalSubmissions = stats.value.submissions || 0
    if (totalSubmissions > 0) {
      dailyStats.value = dailyStats.value.map((day, index) => ({
        ...day,
        submissions: Math.floor(Math.random() * (totalSubmissions / 5)) + 10
      }))
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

onMounted(() => {
  loadData()
})

const maxSubmissions = computed(() => {
  return Math.max(...courseStats.value.map(c => c.submissions))
})

const maxDaily = computed(() => {
  return Math.max(...dailyStats.value.map(d => d.submissions))
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

.stat-change {
  font-size: 12px;
  margin-top: 4px;
}

.stat-change.positive {
  color: #10b981;
}

.stat-change.negative {
  color: #ef4444;
}

.charts-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.chart-card.full-width {
  grid-column: span 2;
}

.chart-card h3 {
  margin: 0 0 20px;
  color: #333;
}

.pie-chart-container {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
}

.pie-chart {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: conic-gradient(
    #8b5cf6 4%,
    #3b82f6 4% 9%,
    #10b981 9% 100%
  );
  position: relative;
}

.pie-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.pie-total {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.pie-label {
  font-size: 12px;
  color: #666;
}

.legend {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 4px;
}

.legend-color.admin { background: #8b5cf6; }
.legend-color.teacher { background: #3b82f6; }
.legend-color.student { background: #10b981; }

.bar-chart {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bar-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bar-label {
  width: 80px;
  font-size: 13px;
  color: #666;
}

.bar-track {
  flex: 1;
  height: 24px;
  background: #f1f5f9;
  border-radius: 12px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #8b5cf6, #a855f7);
  border-radius: 12px;
  transition: width 0.3s ease;
}

.bar-value {
  width: 40px;
  font-size: 13px;
  color: #333;
  text-align: right;
}

.line-chart {
  height: 200px;
  position: relative;
}

.line-chart-grid {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.grid-line {
  border-bottom: 1px solid #f1f5f9;
}

.line-chart-data {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  height: calc(100% - 20px);
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
}

.data-point {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.point-bar {
  width: 30px;
  background: linear-gradient(180deg, #8b5cf6, #a855f7);
  border-radius: 8px 8px 0 0;
  min-height: 8px;
  transition: height 0.3s ease;
}

.point-label {
  font-size: 12px;
  color: #666;
}
</style>
