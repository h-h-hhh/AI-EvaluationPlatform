<template>
  <div class="dashboard-container">
    <AppLayout
      :menu="menu"
      active-path="/admin/users"
      title="用户管理"
      subtitle="管理系统中的所有用户"
      :sidebar-gradient="SIDEBAR_GRADIENT"
      content-scroll
      @logout="logout"
    >
      <template #header-actions>
        <button class="add-btn" @click="showAddModal = true">
          + 添加用户
        </button>
      </template>

      <div class="search-bar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索用户名或姓名..."
          class="search-input"
          size="default"
          clearable
        />
        <el-select v-model="roleFilter" class="filter-select" placeholder="全部角色" size="default">
          <el-option label="全部角色" value="" />
          <el-option label="管理员" value="ADMIN" />
          <el-option label="教师" value="TEACHER" />
          <el-option label="学生" value="STUDENT" />
        </el-select>
      </div>

      <div class="table-container">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户名</th>
              <th>姓名</th>
              <th>邮箱</th>
              <th>角色</th>
              <th>创建时间</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in filteredUsers" :key="user.id">
              <td>{{ user.id }}</td>
              <td>{{ user.username }}</td>
              <td>{{ user.name }}</td>
              <td>{{ user.email }}</td>
              <td>
                <span :class="getRoleClass(user.role)">
                  {{ getRoleText(user.role) }}
                </span>
              </td>
              <td>{{ user.createdAt }}</td>
              <td>
                <span :class="user.active ? 'status-active' : 'status-inactive'">
                  {{ user.active ? '启用' : '禁用' }}
                </span>
              </td>
              <td>
                <button class="action-btn edit" @click="editUser(user)">编辑</button>
                <button 
                  class="action-btn delete" 
                  @click="toggleUserStatus(user)"
                >
                  {{ user.active ? '禁用' : '启用' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="modal-overlay" v-if="showAddModal" @click.self="showAddModal = false">
        <div class="modal-content">
          <h3>{{ isEditing ? '编辑用户' : '添加用户' }}</h3>
          <form @submit.prevent="saveUser">
            <div class="form-group">
              <label>用户名</label>
              <el-input
                v-model="userForm.username"
                required
                class="form-input"
                size="default"
              />
            </div>
            <div class="form-group">
              <label>姓名</label>
              <el-input
                v-model="userForm.name"
                required
                class="form-input"
                size="default"
              />
            </div>
            <div class="form-group">
              <label>邮箱</label>
              <el-input
                v-model="userForm.email"
                type="email"
                required
                class="form-input"
                size="default"
              />
            </div>
            <div class="form-group">
              <label>密码</label>
              <el-input
                v-model="userForm.password"
                type="password"
                :required="!isEditing"
                placeholder="不修改请留空"
                class="form-input"
                size="default"
                show-password
              />
            </div>
            <div class="form-group">
              <label>角色</label>
              <el-select v-model="userForm.role" class="form-select" size="default">
                <el-option label="管理员" value="ADMIN" />
                <el-option label="教师" value="TEACHER" />
                <el-option label="学生" value="STUDENT" />
              </el-select>
            </div>
            <div class="form-actions">
              <button type="button" class="btn-cancel" @click="closeModal">取消</button>
              <button type="submit" class="btn-submit">保存</button>
            </div>
          </form>
        </div>
      </div>
    </AppLayout>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { userApi, authApi } from '../../services/api'
import AppLayout from '../../components/AppLayout.vue'

const router = useRouter()
const route = useRoute()

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
const roleFilter = ref('')
const showAddModal = ref(false)
const isEditing = ref(false)
const editingUserId = ref(null)

const userForm = ref({
  username: '',
  name: '',
  email: '',
  password: '',
  role: 'STUDENT'
})

const users = ref([])

const loadUsers = async () => {
  try {
    const result = await userApi.getAll()
    if (result && result.success && result.data) {
      users.value = result.data
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
  }
}

onMounted(async () => {
  await loadUsers()
  
  const editId = route.query.edit
  if (editId) {
    const user = users.value.find(u => u.id === parseInt(editId))
    if (user) {
      editUser(user)
    }
    router.push('/admin/users')
  }
})

const filteredUsers = computed(() => {
  return users.value.filter(user => {
    const matchSearch = !searchQuery.value || 
      user.username.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      user.name.toLowerCase().includes(searchQuery.value.toLowerCase())
    const matchRole = !roleFilter.value || user.role === roleFilter.value
    return matchSearch && matchRole
  })
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
  isEditing.value = true
  editingUserId.value = user.id
  userForm.value = {
    username: user.username,
    name: user.name,
    email: user.email,
    password: '',
    role: user.role
  }
  showAddModal.value = true
}

const toggleUserStatus = async (user) => {
  if (user.role === 'ADMIN') {
    alert('不能禁用管理员账号')
    return
  }
  try {
    await userApi.toggleStatus(user.id)
    user.active = !user.active
    alert(`${user.name} 已${user.active ? '启用' : '禁用'}`)
  } catch (error) {
    console.error('切换用户状态失败:', error)
    alert('切换用户状态失败，请重试')
  }
}

const saveUser = async () => {
  try {
    if (isEditing.value) {
      const data = {
        username: userForm.value.username,
        name: userForm.value.name,
        email: userForm.value.email,
        role: userForm.value.role
      }
      if (userForm.value.password) {
        data.password = userForm.value.password
      }
      await userApi.update(editingUserId.value, data)
      alert('用户信息更新成功')
    } else {
      await userApi.create(userForm.value)
      alert('用户添加成功')
    }
    await loadUsers()
    closeModal()
  } catch (error) {
    console.error('保存用户失败:', error)
    alert('保存用户失败，请重试')
  }
}

const closeModal = () => {
  showAddModal.value = false
  isEditing.value = false
  editingUserId.value = null
  userForm.value = {
    username: '',
    name: '',
    email: '',
    password: '',
    role: 'STUDENT'
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

.add-btn:hover {
  background: #7c3aed;
}

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

/* 搜索框：flex:1 撑满，外观由 Element Plus 默认主题承担 */
.search-input {
  flex: 1;
}

/* el-input 默认非 100% 宽，需 :deep 穿透撑满 */
.search-input :deep(.el-input) {
  width: 100%;
}

/* 角色筛选下拉：限定宽度避免被 el-select 默认值撑变 */
.filter-select {
  width: 160px;
}

.table-container {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.data-table th {
  color: #666;
  font-weight: 500;
  background: #f8fafc;
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

.status-active {
  color: #10b981;
  font-size: 12px;
}

.status-inactive {
  color: #ef4444;
  font-size: 12px;
}

.action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  margin-right: 8px;
}

.action-btn.edit {
  background: #409eff;
  color: white;
}

.action-btn.edit:hover {
  background: #66b1ff;
}

.action-btn.delete {
  background: #f56c6c;
  color: white;
}

.action-btn.delete:hover {
  background: #f78989;
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
  padding: 24px;
  border-radius: 12px;
  width: 400px;
  max-width: 90%;
}

.modal-content h3 {
  margin: 0 0 20px;
  color: #333;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #333;
  font-size: 14px;
}

/* 表单控件：el-input / el-select 撑满模态框宽度，外观由 Element Plus 默认主题承担 */
.form-input,
.form-select {
  width: 100%;
  box-sizing: border-box;
}

.form-input :deep(.el-input),
.form-select :deep(.el-select) {
  width: 100%;
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

.btn-submit:hover {
  background: #7c3aed;
}
</style>
