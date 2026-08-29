<template>
  <div class="login-container">
    <div class="login-box">
      <h2 class="title">代码作业智慧评价平台</h2>
      <p class="subtitle">基于 LLM + Git 的智能评价系统</p>

      <form @submit.prevent="onSubmit" class="login-form">
        <div class="form-group">
          <label>用户名</label>
          <input
            type="text"
            v-model="username"
            placeholder="请输入用户名"
            class="native-input"
          />
        </div>

        <div class="form-group">
          <label>密码</label>
          <input
            type="password"
            v-model="password"
            placeholder="请输入密码"
            class="native-input"
          />
        </div>

        <button type="submit" class="native-btn">登录</button>
      </form>

      <p class="test-info">
        测试账户: admin / password 或 teacher / password 或 student / password
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const username = ref('')
const password = ref('')

const onSubmit = async () => {
  if (!username.value || !password.value) {
    alert('请输入用户名和密码')
    return
  }

  try {
    const response = await axios.post('/api/auth/login', {
      username: username.value,
      password: password.value
    }, {
      withCredentials: true
    })

    if (response.data.success) {
      const { accessToken, user } = response.data.data
      sessionStorage.setItem('token', accessToken)
      sessionStorage.setItem('role', user.role)
      sessionStorage.setItem('user', JSON.stringify(user))

      // 根据角色跳转到不同页面
      if (user.role === 'ADMIN') {
        router.push('/admin')
      } else if (user.role === 'TEACHER') {
        router.push('/teacher')
      } else if (user.role === 'STUDENT') {
        router.push('/student')
      }
    } else {
      alert('登录失败: ' + response.data.message)
    }
  } catch (error) {
    alert('登录出错: ' + (error.response?.data?.message || error.message))
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  width: 400px;
}

.title {
  text-align: center;
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.subtitle {
  text-align: center;
  color: #666;
  margin-bottom: 30px;
}

.login-form {
  margin-top: 20px;
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

.native-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
  background-color: #fff;
}

.native-input:focus {
  border-color: #667eea;
}

.native-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.native-btn:hover {
  background: #5a6fd6;
}

.test-info {
  margin-top: 16px;
  padding: 10px;
  background: #f5f5f5;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
}
</style>