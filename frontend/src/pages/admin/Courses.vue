<template>
  <div class="dashboard-container">
    <AppLayout
      :menu="menu"
      active-path="/admin/courses"
      title="课程管理"
      subtitle="管理系统中的所有课程"
      :sidebar-gradient="SIDEBAR_GRADIENT"
      content-scroll
      @logout="logout"
    >
      <template #header-actions>
        <button class="add-btn" @click="openAddModal">+ 添加课程</button>
      </template>

      <div class="search-bar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索课程名称..."
          class="search-input"
          size="default"
          clearable
        />
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
              <el-input v-model="courseForm.name" required class="form-input" size="default" />
            </div>
            <div class="form-group">
              <label>课程代码</label>
              <el-input v-model="courseForm.code" required class="form-input" size="default" placeholder="如: CS101" />
            </div>
            <div class="form-group">
              <label>课程描述</label>
              <el-input
                v-model="courseForm.description"
                type="textarea"
                :rows="3"
                class="form-input"
                size="default"
              />
            </div>
            <div class="form-group">
              <label>授课教师</label>
              <el-select v-model="courseForm.teacherId" class="form-select" placeholder="选择教师" size="default">
                <el-option label="选择教师" value="" />
                <el-option
                  v-for="teacher in teachers"
                  :key="teacher.id"
                  :label="teacher.name"
                  :value="String(teacher.id)"
                />
              </el-select>
            </div>
            <div class="form-group">
              <label>课程状态</label>
              <el-select v-model="courseForm.active" class="form-select" size="default">
                <el-option label="进行中" :value="true" />
                <el-option label="已结束" :value="false" />
              </el-select>
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
    </AppLayout>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { courseApi, statisticsApi, authApi } from '../../services/api'
import AppLayout from '../../components/AppLayout.vue'

const router = useRouter()

// 管理端侧边栏菜单（与原 template 硬编码一致：label + path，无 icon）
const menu = [
  { label: '系统看板', path: '/admin' },
  { label: '用户管理', path: '/admin/users' },
  { label: '课程管理', path: '/admin/courses' },
  { label: '数据统计', path: '/admin/statistics' }
]

// 管理端原有紫色侧栏渐变（AppLayout 默认蓝色，此处按原视觉覆盖）
const SIDEBAR_GRADIENT = 'linear-gradient(180deg, #8b5cf6 0%, #6d28d9 100%)'

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
    if (result && result.success && result.data) {
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
    if (result && result.success && result.data) {
      teachers.value = result.data
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
   .header / .header-left h1 / .header-left p 与 AppLayout 现有规则完全等价，故删除。
   注：.main-content 原有 overflow: auto，改由 content-scroll prop 还原；
       .add-btn 为页面特有如按钮样式，保留（按钮经 #header-actions slot 渲染）。 */

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

/* 搜索框：限定宽度，外观由 Element Plus 默认主题承担 */
.search-input {
  width: 300px;
}

.search-input :deep(.el-input) {
  width: 100%;
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

/* 表单控件：撑满模态框宽度，外观由 Element Plus 默认主题承担 */
.form-input,
.form-select {
  width: 100%;
  box-sizing: border-box;
}

.form-input :deep(.el-input),
.form-input :deep(.el-textarea),
.form-select :deep(.el-select) {
  width: 100%;
}

/* textarea 保持原 resize: vertical 高度观感 */
.form-input :deep(.el-textarea__inner) {
  resize: vertical;
}

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
