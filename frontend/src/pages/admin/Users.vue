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
        <div class="nav-item active" @click="$router.push('/admin/users')">
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
        <div class="header-left">
          <h1>用户管理</h1>
          <p>管理系统中的所有用户</p>
        </div>
        <button class="add-btn" @click="showAddModal = true">
          + 添加用户
        </button>
      </header>

      <div class="search-bar">
        <input 
          type="text" 
          v-model="searchQuery" 
          placeholder="搜索用户名或姓名..."
          class="search-input"
        />
        <select v-model="roleFilter" class="filter-select">
          <option value="">全部角色</option>
          <option value="ADMIN">管理员</option>
          <option value="TEACHER">教师</option>
          <option value="STUDENT">学生</option>
        </select>
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
              <input 
                type="text" 
                v-model="userForm.username" 
                required
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label>姓名</label>
              <input 
                type="text" 
                v-model="userForm.name" 
                required
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label>邮箱</label>
              <input 
                type="email" 
                v-model="userForm.email" 
                required
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label>密码</label>
              <input 
                type="password" 
                v-model="userForm.password" 
                :required="!isEditing"
                placeholder="不修改请留空"
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label>角色</label>
              <select v-model="userForm.role" class="form-select">
                <option value="ADMIN">管理员</option>
                <option value="TEACHER">教师</option>
                <option value="STUDENT">学生</option>
              </select>
            </div>
            <div class="form-actions">
              <button type="button" class="btn-cancel" @click="closeModal">取消</button>
              <button type="submit" class="btn-submit">保存</button>
            </div>
          </form>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { userApi } from '../../services/api'

const router = useRouter()
const route = useRoute()

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
    if (result && result.data) {
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

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.header-left h1 {
  margin: 0 0 5px;
  color: #333;
}

.header-left p {
  color: #666;
  margin: 0;
}

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

.search-input {
  flex: 1;
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
}

.filter-select {
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
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

.form-input,
.form-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
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
