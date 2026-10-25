import { createRouter, createWebHistory } from 'vue-router'
import axios from 'axios'

const routes = [
  {
    path: '/',
    name: 'Login',
    component: () => import('../pages/Login.vue')
  },
  // 本系统关闭公开注册，所有账号由管理员后台创建；/register 路径自动跳回登录页
  {
    path: '/register',
    redirect: '/'
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('../pages/admin/Dashboard.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' }
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('../pages/admin/Users.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' }
  },
  {
    path: '/admin/courses',
    name: 'AdminCourses',
    component: () => import('../pages/admin/Courses.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' }
  },
  {
    path: '/admin/statistics',
    name: 'AdminStatistics',
    component: () => import('../pages/admin/Statistics.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' }
  },
  {
    path: '/teacher',
    name: 'TeacherDashboard',
    component: () => import('../pages/teacher/Dashboard.vue'),
    meta: { requiresAuth: true, role: 'TEACHER' }
  },
  {
    path: '/teacher/courses',
    name: 'TeacherCourses',
    component: () => import('../pages/teacher/Courses.vue'),
    meta: { requiresAuth: true, role: 'TEACHER' }
  },
  {
    path: '/teacher/assignments',
    name: 'TeacherAssignments',
    component: () => import('../pages/teacher/Assignments.vue'),
    meta: { requiresAuth: true, role: 'TEACHER' }
  },
  {
    path: '/teacher/evaluations',
    name: 'TeacherEvaluations',
    component: () => import('../pages/teacher/Evaluations.vue'),
    meta: { requiresAuth: true, role: 'TEACHER' }
  },
  {
    path: '/student',
    name: 'StudentDashboard',
    component: () => import('../pages/student/Dashboard.vue'),
    meta: { requiresAuth: true, role: 'STUDENT' }
  },
  {
    path: '/student/courses',
    name: 'StudentCourses',
    component: () => import('../pages/student/Courses.vue'),
    meta: { requiresAuth: true, role: 'STUDENT' }
  },
  {
    path: '/student/submissions',
    name: 'StudentSubmissions',
    component: () => import('../pages/student/Submissions.vue'),
    meta: { requiresAuth: true, role: 'STUDENT' }
  },
  {
    path: '/student/results',
    name: 'StudentResults',
    component: () => import('../pages/student/Results.vue'),
    meta: { requiresAuth: true, role: 'STUDENT' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const token = sessionStorage.getItem('token')
  const userRole = sessionStorage.getItem('role')

  if (to.meta.requiresAuth) {
    if (!token || !userRole) {
      try {
        const refreshResponse = await axios.post('/api/auth/refresh', {}, { withCredentials: true })
        if (refreshResponse.data.success) {
          const { accessToken, user } = refreshResponse.data.data
          sessionStorage.setItem('token', accessToken)
          sessionStorage.setItem('role', user.role)
          sessionStorage.setItem('user', JSON.stringify(user))
          
          if (to.meta.role && user.role !== to.meta.role) {
            let targetPath = '/'
            if (user.role === 'ADMIN') {
              targetPath = '/admin'
            } else if (user.role === 'TEACHER') {
              targetPath = '/teacher'
            } else if (user.role === 'STUDENT') {
              targetPath = '/student'
            }
            window.location.href = targetPath
            return
          }
          next()
          return
        }
      } catch (refreshError) {
        console.log('Refresh token expired or invalid')
      }
      window.location.href = '/'
      return
    }
    if (to.meta.role) {
      if (userRole !== to.meta.role) {
        let targetPath = '/'
        if (userRole === 'ADMIN') {
          targetPath = '/admin'
        } else if (userRole === 'TEACHER') {
          targetPath = '/teacher'
        } else if (userRole === 'STUDENT') {
          targetPath = '/student'
        }
        window.location.href = targetPath
        return
      }
    }
  }
  next()
})

export default router
