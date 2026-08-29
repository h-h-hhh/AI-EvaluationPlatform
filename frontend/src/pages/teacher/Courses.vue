<template>
  <div class="courses-container">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="logo">
        <h2>代码评价平台</h2>
      </div>
      <nav class="sidebar-nav">
        <div class="nav-item" @click="$router.push('/teacher')">
          数据看板
        </div>
        <div class="nav-item active" @click="$router.push('/teacher/courses')">
          课程管理
        </div>
        <div class="nav-item" @click="$router.push('/teacher/assignments')">
          作业管理
        </div>
        <div class="nav-item" @click="$router.push('/teacher/evaluations')">
          评价分析
        </div>
      </nav>
      <div class="logout-btn" @click="logout">
        退出登录
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <header class="header">
        <h1>课程管理</h1>
        <button class="primary-btn" @click="openCreateModal">
          + 新建课程
        </button>
      </header>

      <!-- 课程列表 -->
      <div class="courses-section">
        <table class="simple-table">
          <thead>
            <tr>
              <th>课程名称</th>
              <th>课程代码</th>
              <th>课程描述</th>
              <th>学生人数</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="course in courses" :key="course.id">
              <td>{{ course.name }}</td>
              <td>{{ course.code }}</td>
              <td>{{ course.description }}</td>
              <td>{{ course.studentCount }}</td>
              <td>
                <span class="status-active">{{ course.status }}</span>
              </td>
              <td>
                <button class="action-btn" @click="editCourse(course)">编辑</button>
                <button class="action-btn danger" @click="deleteCourse(course)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 课程弹窗（新建/编辑） -->
      <div v-if="showModal" class="modal-overlay">
        <div class="modal-content">
          <h3>{{ isEditing ? '编辑课程' : '新建课程' }}</h3>
          <div class="form-group">
            <label>课程名称 *</label>
            <input v-model="courseForm.name" placeholder="请输入课程名称" />
          </div>
          <div class="form-group">
            <label>课程代码 *</label>
            <input v-model="courseForm.code" placeholder="请输入课程代码" />
          </div>
          <div class="form-group">
            <label>课程描述</label>
            <textarea v-model="courseForm.description" placeholder="请输入课程描述" rows="3"></textarea>
          </div>
          <div class="modal-actions">
            <button class="secondary-btn" @click="closeModal">取消</button>
            <button class="primary-btn" @click="saveCourse">{{ isEditing ? '保存' : '确定' }}</button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { courseApi, authApi } from '../../services/api'

const router = useRouter()

const showModal = ref(false)
const isEditing = ref(false)
const editingCourseId = ref(null)
const courseForm = reactive({
  name: '',
  code: '',
  description: ''
})

const courses = ref([])

const loadCourses = async () => {
  try {
    const userStr = sessionStorage.getItem('user')
    if (!userStr) return
    
    const user = JSON.parse(userStr)
    const response = await courseApi.getByTeacher(user.id)
    if (response && response.success && response.data) {
      courses.value = response.data.map(course => ({
        id: course.id,
        name: course.name,
        code: course.code,
        description: course.description || '',
        studentCount: 0,
        status: course.active ? '活跃' : '已结束'
      }))
    }
  } catch (error) {
    console.error('加载课程失败:', error)
  }
}

onMounted(() => {
  loadCourses()
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

const openCreateModal = () => {
  isEditing.value = false
  editingCourseId.value = null
  courseForm.name = ''
  courseForm.code = ''
  courseForm.description = ''
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  isEditing.value = false
  editingCourseId.value = null
  courseForm.name = ''
  courseForm.code = ''
  courseForm.description = ''
}

const editCourse = (course) => {
  isEditing.value = true
  editingCourseId.value = course.id
  courseForm.name = course.name
  courseForm.code = course.code
  courseForm.description = course.description
  showModal.value = true
}

const saveCourse = async () => {
  if (!courseForm.name || !courseForm.code) {
    alert('请填写必填字段')
    return
  }

  try {
    let response
    if (isEditing.value) {
      response = await courseApi.update(editingCourseId.value, courseForm)
    } else {
      response = await courseApi.create(courseForm)
    }
    
    if (response.success) {
      closeModal()
      loadCourses()
      alert(isEditing.value ? '课程更新成功' : '课程创建成功')
    } else {
      alert(isEditing.value ? '更新失败: ' + response.message : '创建失败: ' + response.message)
    }
  } catch (error) {
    alert(isEditing.value ? '更新失败: ' : '创建失败: ' + (error.response?.data?.message || error.message))
  }
}

const deleteCourse = async (course) => {
  if (confirm(`确定删除课程 ${course.name}？`)) {
    try {
      const response = await courseApi.delete(course.id)
      if (response.success) {
        courses.value = courses.value.filter(c => c.id !== course.id)
        alert('课程删除成功')
      } else {
        alert('删除失败: ' + response.message)
      }
    } catch (error) {
      alert('删除失败: ' + (error.response?.data?.message || error.message))
    }
  }
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
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h1 {
  margin: 0;
  color: #333;
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
  background: #fff;
  color: #333;
  border: 1px solid #ddd;
  cursor: pointer;
  border-radius: 4px;
}

.secondary-btn:hover {
  background: #f5f5f5;
}

.courses-section {
  background: white;
  padding: 20px;
  border-radius: 12px;
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
}

.status-active {
  color: #52c41a;
}

.action-btn {
  padding: 6px 12px;
  background: #409eff;
  color: white;
  border: none;
  cursor: pointer;
  border-radius: 4px;
  margin-right: 8px;
}

.action-btn:hover {
  background: #66b1ff;
}

.action-btn.danger {
  background: #ff4d4f;
}

.action-btn.danger:hover {
  background: #ff7875;
}

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
}

.modal-content {
  background: white;
  padding: 30px;
  border-radius: 12px;
  width: 500px;
  max-width: 90%;
}

.modal-content h3 {
  margin: 0 0 20px;
  color: #333;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.form-group textarea {
  resize: vertical;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
</style>
