<template>
  <div class="dashboard-container">
    <AppLayout
      :menu="menu"
      active-path="/student"
      title="学生学习看板"
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
    </AppLayout>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { courseApi, assignmentApi, authApi } from '../../services/api'
import AppLayout from '../../components/AppLayout.vue'

const router = useRouter()

// 学生端侧边栏菜单（与原 template 硬编码一致：label + path，无 icon）
const menu = [
  { label: '学习看板', path: '/student' },
  { label: '我的课程', path: '/student/courses' },
  { label: '作业提交', path: '/student/submissions' },
  { label: '评价结果', path: '/student/results' }
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
  submitted: 0,
  evaluated: 0
})

const recentAssignments = ref([])

/**
 * 简化版作业状态判定（Dashboard 无"已提交"概念，只区分进行中/已截止）
 * 与 Submissions.vue 的 parseDeadline 语义保持一致：解析失败时兜底为 '进行中'
 */
const computeAssignmentStatus = (deadline) => {
  if (!deadline) return '进行中'
  let normalized = deadline
  if (typeof normalized === 'string' && /^\d{4}-\d{2}-\d{2} /.test(normalized)) {
    normalized = normalized.replace(' ', 'T')
  }
  const d = Array.isArray(normalized)
    ? (() => { const [y, mo, dd, hh = 0, mi = 0, se = 0] = normalized; return new Date(y, (mo || 1) - 1, dd || 1, hh, mi, se) })()
    : new Date(normalized)
  if (isNaN(d.getTime())) return '进行中'
  return new Date() > d ? '已截止' : '进行中'
}

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
        // 2026-08-31：AssignmentDTO 平铺字段，兼容老实体
        course: a.courseName ?? a.course?.name ?? '',
        deadline: a.deadline ? String(a.deadline).replace('T', ' ') : '',
        // Bug 修复：不再 hard-code '待提交'，按实际截止时间判定
        status: computeAssignmentStatus(a.deadline)
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