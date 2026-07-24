import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

api.interceptors.request.use(
  config => {
    const token = sessionStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    if (error.response && error.response.status === 401) {
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('role')
      sessionStorage.removeItem('user')
      window.location.href = '/'
    }
    return Promise.reject(error)
  }
)

export const authApi = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data)
}

export const courseApi = {
  getAll: () => api.get('/courses'),
  getById: (id) => api.get(`/courses/${id}`),
  create: (data) => api.post('/courses', data),
  update: (id, data) => api.put(`/courses/${id}`, data),
  delete: (id) => api.delete(`/courses/${id}`),
  getByTeacher: (teacherId) => api.get(`/courses/teacher/${teacherId}`),
  getEnrolled: () => api.get('/courses/student/enrolled'),
  getAvailable: () => api.get('/courses/student/available'),
  enroll: (courseId) => api.post(`/courses/student/enroll/${courseId}`),
  drop: (courseId) => api.post(`/courses/student/drop/${courseId}`)
}

export const assignmentApi = {
  getAll: () => api.get('/assignments'),
  getById: (id) => api.get(`/assignments/${id}`),
  create: (data) => api.post('/assignments', data),
  update: (id, data) => api.put(`/assignments/${id}`, data),
  delete: (id) => api.delete(`/assignments/${id}`),
  getByCourse: (courseId) => api.get(`/assignments/course/${courseId}`)
}

export const submissionApi = {
  getById: (id) => api.get(`/submissions/${id}`),
  getByAssignment: (assignmentId) => api.get(`/submissions/assignment/${assignmentId}`),
  getByStudent: (studentId) => api.get(`/submissions/student/${studentId}`),
  getLatest: (assignmentId) => api.get(`/submissions/latest/${assignmentId}`),
  submit: (data) => api.post('/submissions', data)
}

export const evaluationApi = {
  getById: (id) => api.get(`/evaluations/${id}`),
  getBySubmission: (submissionId) => api.get(`/evaluations/submission/${submissionId}`),
  getByAssignment: (assignmentId) => api.get(`/evaluations/assignment/${assignmentId}`),
  getByStudent: (studentId) => api.get(`/evaluations/student/${studentId}`),
  evaluate: (submissionId) => api.post(`/evaluations/${submissionId}`)
}

export const gitApi = {
  validateUrl: (data) => api.post('/git/validate', data),
  getSubmissionHistory: (studentId, assignmentId) => api.get(`/git/submission-history?studentId=${studentId}&assignmentId=${assignmentId}`),
  getRepositoryAnalysis: (assignmentId) => api.get(`/git/repository-analysis?assignmentId=${assignmentId}`)
}

export const userApi = {
  getAll: () => api.get('/admin/users'),
  getById: (id) => api.get(`/admin/users/${id}`),
  create: (data) => api.post('/admin/users', data),
  update: (id, data) => api.put(`/admin/users/${id}`, data),
  delete: (id) => api.delete(`/admin/users/${id}`),
  toggleStatus: (id) => api.put(`/admin/users/${id}/status`)
}

export const statisticsApi = {
  getOverview: () => api.get('/statistics/overview'),
  getUserRoleDistribution: () => api.get('/statistics/user-role-distribution'),
  getCourseSubmissions: () => api.get('/statistics/course-submissions'),
  getRecentUsers: () => api.get('/statistics/recent-users'),
  getCourseList: () => api.get('/statistics/course-list')
}

export default api
