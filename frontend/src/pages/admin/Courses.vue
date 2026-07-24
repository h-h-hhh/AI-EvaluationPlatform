<template>
  <div class="dashboard-container">
    <aside class="sidebar">
      <div class="logo">
        <h2>代码评价平台</h2>
      </div>
      <nav class="sidebar-nav">
        <div class="nav-item" @click="$router.push('/admin')">系统看板</div>
        <div class="nav-item" @click="$router.push('/admin/users')">用户管理</div>
        <div class="nav-item active" @click="$router.push('/admin/courses')">课程管理</div>
        <div class="nav-item" @click="$router.push('/admin/statistics')">数据统计</div>
      </nav>
      <div class="logout-btn" @click="logout">退出登录</div>
    </aside>

    <main class="main-content">
      <header class="header">
        <div class="header-left">
          <h1>课程管理</h1>
          <p>管理系统中的所有课程</p>
        </div>
        <button class="add-btn" @click="openAddModal">+ 添加课程</button>
      </header>

      <div class="search-bar">
        <input type="text" v-model="searchQuery" placeholder="搜索课程名称..." class="search-input" />
      </div>

      <div class="cards-grid">
        <div v-for="course in filteredCourses" :key="course.id" class="course-card">
          <div class="course-header">
            <h3>{{ course.name }}</h3>
            <span :class="course.active ? 'status-badge active' : 'status-badge inactive'">
              {{ course.active ? '进行中' : '已结束' }}
            </span>
          </div>
          <p class="course-desc">{{ course.description }}</p>
          <div class="course-meta">
            <div class="meta-item"><span class="meta-icon">👨‍🏫</span><span>{{ course.teacher }}</span></div>
            <div class="meta-item"><span class="meta-icon">🧑‍🎓</span><span>{{ course.studentCount }} 名学生</span></div>
            <div class="meta-item"><span class="meta-icon">📝</span><span>{{ course.assignmentCount }} 个作业</span></div>
          </div>
          <div class="course-actions">
            <button class="action-btn" @click="openEditModal(course)">编辑</button>
            <button class="action-btn" :class="course.active ? 'secondary' : 'primary'" @click="deleteCourse(course)">
              {{ course.active ? '结束' : '开启' }}
            </button>
          </div>
        </div>
      </div>

      <div class="modal-overlay" v-if="showModal" @click.self="closeModal">
        <div class="modal-content">
          <h3>{{ isEditing ? '编辑课程' : '添加课程' }}</h3>
          <form @submit.prevent="saveCourse">
            <div class="form-group">
              <label>课程名称</label>
              <input type="text" v-model="courseForm.name" required class="form-input" />
            </div>
            <div class="form-group">
              <label>课程代码</label>
              <input type="text" v-model="courseForm.code" required class="form-input" placeholder="如: CS101" />
            </div>
            <div class="form-group">
              <label>课程描述</label>
              <textarea v-model="courseForm.description" rows="3" class="form-input"></textarea>
            </div>
            <div class="form-group">
              <label>授课教师</label>
              <select v-model="courseForm.teacherId" class="form-select">
                <option value="">选择教师</option>
                <option v-for="teacher in teachers" :key="teacher.id" :value="teacher.id">{{ teacher.name }}</option>
              </select>
            </div>
            <div class="form-group">
              <label>课程状态</label>
              <select v-model="courseForm.active" class="form-select">
                <option :value="true">进行中</option>
                <option :value="false">已结束</option>
              </select>
            </div>
            <div class="form-actions">
              <button type="button" class="btn-cancel" @click="closeModal">取消</button>
              <button type="submit" class="btn-submit">保存</button>
            </div>
          </form>
        </div>
      </div>

      <div class="modal-overlay" v-if="showDeleteConfirm" @click.self="cancelDelete">
        <div class="modal-content">
          <h3>确认删除</h3>
          <p>确定要结束课程 "{{ deletingCourseData?.name }}" 吗？结束后课程将被删除。</p>
          <div class="form-actions">
            <button type="button" class="btn-cancel" @click="cancelDelete">取消</button>
            <button type="button" class="btn-delete" @click="confirmDelete">确认删除</button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { courseApi, statisticsApi } from '../../services/api'

const router = useRouter()

const searchQuery = ref('')
const showModal = ref(false)
const isEditing = ref(false)
const editingCourseId = ref(null)
const showDeleteConfirm = ref(false)
const deletingCourseData = ref(null)

const teachers = ref([])

const courseForm = ref({
  name: '',
  description: '',
  code: '',
  teacherId: '',
  active: true
})

const courses = ref([])

const loadCourses = async () => {
  try {
    const result = await courseApi.getAll()
    if (result && result.data) {
      courses.value = result.data.map(course => ({
        ...course,
        teacher: course.teacher?.name || '未知教师',
        teacherId: course.teacher?.id || course.teacherId,
        studentCount: 0,
        assignmentCount: 0
      }))
    }
  } catch (error) {
    console.error('加载课程列表失败:', error)
  }
}

const loadTeachers = async () => {
  try {
    const result = await statisticsApi.getRecentUsers()
    if (result) {
      teachers.value = result
        .filter(user => user.role === 'TEACHER')
        .map(user => ({ id: user.id, name: user.name }))
    }
  } catch (error) {
    console.error('加载教师列表失败:', error)
  }
}

onMounted(() => {
  loadCourses()
  loadTeachers()
})

const filteredCourses = computed(() => {
  return courses.value.filter(course => {
    return !searchQuery.value || 
      course.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      course.description.toLowerCase().includes(searchQuery.value.toLowerCase())
  })
})

const openAddModal = () => {
  isEditing.value = false
  editingCourseId.value = null
  courseForm.value = { name: '', description: '', code: '', teacherId: '', active: true }
  showModal.value = true
}

const openEditModal = (course) => {
  isEditing.value = true
  editingCourseId.value = course.id
  courseForm.value = {
    name: course.name,
    description: course.description,
    code: course.code || '',
    teacherId: course.teacherId ? course.teacherId.toString() : '',
    active: course.active
  }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  isEditing.value = false
  editingCourseId.value = null
  courseForm.value = { name: '', description: '', code: '', teacherId: '', active: true }
}

const deleteCourse = (course) => {
  if (!course.active) {
    alert('已结束的课程无法重新开启，请联系管理员')
    return
  }
  deletingCourseData.value = course
  showDeleteConfirm.value = true
}

const cancelDelete = () => {
  showDeleteConfirm.value = false
  deletingCourseData.value = null
}

const confirmDelete = async () => {
  if (!deletingCourseData.value) return
  
  const course = deletingCourseData.value
  showDeleteConfirm.value = false
  deletingCourseData.value = null
  
  try {
    const response = await courseApi.delete(course.id)
    if (response.success) {
      courses.value = courses.value.filter(c => c.id !== course.id)
    }
  } catch (error) {
    console.error('删除课程失败:', error)
  }
}

const saveCourse = async () => {
  const teacher = teachers.value.find(t => t.id.toString() === courseForm.value.teacherId)
  const teacherName = teacher ? teacher.name : '未知教师'
  
  try {
    if (isEditing.value) {
      await courseApi.update(editingCourseId.value, courseForm.value)
      const index = courses.value.findIndex(c => c.id === editingCourseId.value)
      if (index !== -1) {
        courses.value[index] = {
          ...courses.value[index],
          ...courseForm.value,
          teacher: teacherName,
          teacherId: parseInt(courseForm.value.teacherId) || courses.value[index].teacherId
        }
      }
    } else {
      const response = await courseApi.create(courseForm.value)
      if (response.success && response.data) {
        courses.value.push({
          ...response.data,
          teacher: teacherName,
          studentCount: 0,
          assignmentCount: 0
        })
      }
    }
    closeModal()
  } catch (error) {
    console.error('保存课程失败:', error)
    alert('保存失败，请重试')
  }
}

const logout = () => {
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

.logo h2 { margin: 0; font-size: 18px; }

.sidebar-nav { flex: 1; padding: 10px 0; }

.nav-item {
  padding: 12px 20px;
  cursor: pointer;
  transition: background 0.2s;
}

.nav-item:hover, .nav-item.active { background: rgba(255, 255, 255, 0.1); }

.logout-btn {
  padding: 15px 20px;
  cursor: pointer;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.logout-btn:hover { background: rgba(255, 255, 255, 0.1); }

.main-content { flex: 1; padding: 20px; overflow: auto; }

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.header-left h1 { margin: 0 0 5px; color: #333; }

.header-left p { color: #666; margin: 0; }

.add-btn {
  padding: 10px 20px;
  background: #8b5cf6;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.add-btn:hover { background: #7c3aed; }

.search-bar { margin-bottom: 20px; }

.search-input {
  width: 300px;
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.course-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.course-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.course-header h3 { margin: 0; color: #333; }

.status-badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
}

.status-badge.active { background: #d1fae5; color: #10b981; }

.status-badge.inactive { background: #fef3c7; color: #f59e0b; }

.course-desc {
  color: #666;
  font-size: 14px;
  margin: 0 0 15px;
  line-height: 1.5;
}

.course-meta {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #666;
}

.meta-icon { font-size: 16px; }

.course-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  flex: 1;
  padding: 8px 12px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.action-btn.primary { background: #8b5cf6; color: white; }

.action-btn.primary:hover { background: #7c3aed; }

.action-btn.secondary { background: #f56c6c; color: white; }

.action-btn.secondary:hover { background: #f78989; }

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
  width: 450px;
  max-width: 90%;
}

.modal-content h3 { margin: 0 0 20px; color: #333; }

.form-group { margin-bottom: 16px; }

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #333;
  font-size: 14px;
}

.form-input, .form-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}

textarea.form-input { resize: vertical; }

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.btn-cancel {
  padding: 10px 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.btn-submit {
  padding: 10px 20px;
  background: #8b5cf6;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.btn-submit:hover { background: #7c3aed; }

.btn-delete {
  padding: 10px 20px;
  background: #f56c6c;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.btn-delete:hover { background: #e45656; }
</style>
