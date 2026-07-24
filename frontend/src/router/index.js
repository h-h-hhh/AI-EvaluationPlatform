import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Login',
    component: () => import('../pages/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../pages/Register.vue')
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

router.beforeEach((to, from, next) => {
  const token = sessionStorage.getItem('token')
  const userRole = sessionStorage.getItem('role')

  if (to.meta.requiresAuth) {
    if (!token) {
      window.location.href = '/'
      return
    }
    if (!userRole) {
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
