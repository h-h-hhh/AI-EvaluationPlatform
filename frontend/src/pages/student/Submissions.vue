<template>
  <div class="submissions-container">
    <AppLayout
      :menu="menu"
      active-path="/student/submissions"
      title="作业提交"
      subtitle="通过Git提交您的代码作业"
      @logout="handleLogout"
    >
      <div class="filter-bar">
        <el-select v-model="selectedCourse" class="native-select" placeholder="全部课程" @change="loadAssignments">
          <el-option label="全部课程" value="" />
          <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
        </el-select>
        <el-select v-model="statusFilter" class="native-select" placeholder="全部状态">
          <el-option label="全部状态" value="" />
          <el-option label="进行中" value="进行中" />
          <el-option label="已提交" value="已提交" />
          <el-option label="已截止" value="已截止" />
        </el-select>
      </div>
      
      <div class="assignments-list">
        <div v-for="assignment in filteredAssignments" :key="assignment.id" class="assignment-card">
          <div class="assignment-header">
            <div class="assignment-info">
              <h3>{{ assignment.title }}</h3>
              <p>{{ assignment.courseName }}</p>
            </div>
            <span :class="getStatusClass(assignment.status)">
              {{ assignment.status }}
            </span>
          </div>
          
          <div class="assignment-details">
            <div class="detail-item">
              <span>截止日期: {{ formatDeadline(assignment.deadline) }}</span>
            </div>
            <div class="detail-item">
              <span>总分: {{ assignment.totalScore }}分</span>
            </div>
            <div class="detail-item">
              <span>提交次数: {{ assignment.submissionCount || 0 }}次</span>
            </div>
          </div>
          
          <p class="assignment-description">{{ assignment.description }}</p>
          
          <div v-if="assignment.status !== '已截止'" class="submit-section">
            <!-- GitHub 仓库提交（唯一提交方式）：输入公开仓库 URL + 分支（可选）；端点 POST /api/submissions/github -->
            <div class="git-submit-form">
              <div class="form-group">
                <label>GitHub 仓库地址 <span class="req">*</span></label>
                <el-input
                  v-model="assignment.githubUrl"
                  placeholder="https://github.com/用户名/仓库名，公开仓库"
                  size="default"
                  clearable
                  :disabled="assignment.status === '已提交' || assignment.submitting"
                  @blur="normalizeGithubInput(assignment)"
                />
                <small class="hint">支持 https://github.com/user/repo（自动识别 main / master 分支）或带 /tree/xxx 的路径。私有仓库暂不支持。</small>
              </div>
              <div class="form-group">
                <label>分支名称（可选）</label>
                <el-input
                  v-model="assignment.branch"
                  placeholder="留空则自动尝试 main，其次 master"
                  size="default"
                  :disabled="assignment.status === '已提交' || assignment.submitting"
                />
              </div>
              <div class="form-group">
                <label>提交说明</label>
                <el-input
                  v-model="assignment.commitMessage"
                  type="textarea"
                  :rows="2"
                  placeholder="对本次提交的简短说明（可选）"
                  :disabled="assignment.status === '已提交'"
                />
              </div>

              <div v-if="assignment.githubInfo" class="github-submit-info">
                <div class="status-icon success">✓</div>
                <div class="status-text">
                  <p><strong>GitHub 仓库已解析</strong></p>
                  <p>分支：{{ assignment.githubInfo.branch }} · 抽取 {{ assignment.githubInfo.fileCount }} 个代码文件
                     <span v-if="assignment.githubInfo.skippedCount"> · {{ assignment.githubInfo.skippedCount }} 个非代码文件跳过</span></p>
                </div>
              </div>

              <div v-if="assignment.status === '已提交'" class="git-status-info">
                <div class="status-icon">
                  <span class="icon-success">✓</span>
                </div>
                <div class="status-text">
                  <p><strong>已提交</strong> - 提交时间: {{ assignment.submittedTime }}</p>
                  <p v-if="assignment.githubUrl"><a :href="assignment.githubUrl" target="_blank" rel="noopener">{{ assignment.githubUrl }} @ {{ assignment.githubBranch || 'default' }}</a></p>
                </div>
              </div>

              <div class="submit-actions">
                <button
                  v-if="assignment.status === '已提交'"
                  class="warning-btn"
                  @click="resubmitAssignment(assignment)"
                  :disabled="assignment.submitting"
                >
                  重新提交
                </button>
                <button
                  v-else
                  class="primary-btn"
                  :disabled="assignment.submitting"
                  @click="submitGithub(assignment)"
                >
                  {{ assignment.submitting ? '下载仓库中…' : '提交 GitHub 仓库并评分' }}
                </button>
              </div>
            </div>
          </div>

          <div v-else class="submitted-info">
            <div class="submitted-status">
              <span class="status-icon success">✓</span>
              <div class="status-details">
                <p><strong>作业已截止</strong></p>
                <p>截止时间: {{ formatDeadline(assignment.deadline) }}</p>
              </div>
            </div>
          </div>
          
          <!-- Phase 2：评价状态区域，按异步任务状态展示三种反馈（评价中 / 失败可重试 / 已完成） -->
          <div v-if="assignment.evaluationStatus === 'PENDING' || assignment.evaluationStatus === 'PROCESSING'" class="evaluation-section">
            <div class="eval-header">
              <span class="eval-badge">
                <span class="loading-dot"></span>
                {{ assignment.evaluationStatus === 'PENDING' ? '排队等待评价' : 'AI 评价中' }}
              </span>
              <span class="eval-score">DeepSeek 正在分析代码，预计 1~2 分钟…</span>
            </div>
            <!-- Element Plus indeterminate 进度条：滑动动画表达"进行中"，不承诺具体百分比 -->
            <el-progress
              class="eval-progress"
              :percentage="50"
              :indeterminate="true"
              :duration="2"
              :show-text="false"
            />
          </div>

          <div v-else-if="assignment.evaluationStatus === 'FAILED'" class="evaluation-section evaluation-failed">
            <div class="eval-header">
              <span class="eval-badge eval-badge-failed">评价失败</span>
              <span class="eval-score">{{ assignment.evaluationError || '原因未知，请重试' }}</span>
            </div>
            <!-- 重试按钮：loading 期间禁用，防止重复触发 -->
            <button class="action-btn" :disabled="assignment.retrying" @click="retryEvaluation(assignment)">
              {{ assignment.retrying ? '重试中…' : '重新评价' }}
            </button>
          </div>

          <div v-else-if="assignment.evaluated" class="evaluation-section">
            <div class="eval-header">
              <span class="eval-badge">评价完成</span>
              <span class="eval-score">得分: {{ assignment.finalScore }}</span>
            </div>
            <button class="action-btn" @click="viewEvaluation(assignment)">查看评价详情</button>
          </div>
        </div>
        
        <div v-if="filteredAssignments.length === 0" class="empty-state">
          <div class="empty-icon">📝</div>
          <p>没有找到作业</p>
          <p class="empty-hint">请选择课程查看作业列表</p>
        </div>
      </div>
    </AppLayout>

    <div v-if="showSuccessToast" class="success-toast">
      <span class="toast-icon">✓</span>
      <span class="toast-message">{{ toastMessage }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { assignmentApi, courseApi, submissionApi, evaluationApi, authApi } from '../../services/api'
import { useEvaluationPolling } from '../../composables/useEvaluationPolling'
import AppLayout from '../../components/AppLayout.vue'

const router = useRouter()
const route = useRoute()

// 学生端侧边栏菜单（与原 template 硬编码一致：label + path，无 icon）
const menu = [
  { label: '学习看板', path: '/student' },
  { label: '我的课程', path: '/student/courses' },
  { label: '作业提交', path: '/student/submissions' },
  { label: '评价结果', path: '/student/results' }
]

const selectedCourse = ref('')
const statusFilter = ref('')
const showSuccessToast = ref(false)
const toastMessage = ref('')

const courses = ref([])
const assignments = ref([])
const currentUser = ref(null)

// ==================== Phase 2：AI 评价轮询 ====================
// 轮询参数：3s 起步、1.5 倍退避（上限 5s）、最多 60 次判定超时（约 3~5 分钟）
const { start: startPolling } = useEvaluationPolling({
  interval: 3000,
  maxInterval: 5000,
  maxAttempts: 60
})

const loadCourses = async () => {
  try {
    const response = await courseApi.getEnrolled()
    if (response && response.success && response.data) {
      courses.value = response.data.map(c => ({ id: c.id, name: c.name }))
    }
  } catch (error) {
    console.error('加载课程失败:', error)
  }
}

const loadAssignments = async () => {
  try {
    const response = await assignmentApi.getAll()
    if (response && response.success && response.data) {
      assignments.value = response.data.map(a => {
        // 2026-08-31：后端已改为 AssignmentDTO 平铺字段（courseId/courseName/deadline 已是标量字符串），
        // 兼容老实体字段（a.course 嵌套对象），两种格式均正常
        const item = {
          id: a.id,
          title: a.title,
          courseId: a.courseId ?? a.course?.id,
          courseName: a.courseName ?? a.course?.name ?? '',
          deadline: a.deadline,
          totalScore: a.totalScore,
          description: a.description,
          gitEnabled: true,
          // 原有旧字段兼容：gitUrl 保留（老逻辑提交时把 GitHub Tab 的输入映射到 githubUrl）
          gitUrl: '',
          // 【新增 GitHub 提交】学生填写的原始 URL；提交成功后后端会规范化为仓库首页 URL 回填
          githubUrl: '',
          branch: 'main',
          commitMessage: '',
          // 【新增】后端从 GitHub 抽取完成后返回的解析结果附加信息（fileCount/branch/url）
          githubInfo: null,
          submittedTime: null,
          submissionCount: 0,
          evaluated: false,
          finalScore: 0,
          submissionId: null,
          evaluationStatus: null,
          evaluationError: '',
          submitting: false,
          retrying: false
        }
        item.status = getAssignmentStatus(item)
        return item
      })
    }
  } catch (error) {
    console.error('加载作业失败:', error)
  }
}

/**
 * 健壮地解析作业截止时间为 Date 对象
 *
 * Spring Boot 3 + jackson-datatype-jsr310 对 LocalDateTime 默认序列化格式：
 *   "2026-09-01T12:00:00"（ISO 本地字符串，无 TZ）→ new Date 按本地时区解析
 * 这里做了 3 层兜底，防止未来切换序列化配置时再次出 bug：
 *   1. Date 对象 → 直接返回
 *   2. 数组形式 [年,月,日,时,分,秒]（WRITE_DATES_AS_TIMESTAMPS=true）→ 手动构造
 *   3. 字符串 → "yyyy-MM-dd HH:mm:ss"（无 T）先补 T 再构造，ISO 串直接构造
 *
 * 返回 null 表示解析失败（视为"未设置截止时间"）
 */
const parseDeadline = (raw) => {
  if (!raw) return null
  if (raw instanceof Date) return raw
  try {
    let d
    if (Array.isArray(raw)) {
      // Jackson 数组格式：月份 1-based，与 LocalDateTime.of 语义一致
      const [y, mo, dd, hh = 0, mi = 0, se = 0] = raw
      d = new Date(y, (mo || 1) - 1, dd || 1, hh, mi, se)
    } else if (typeof raw === 'string') {
      // 前端 formatDeadline/教师端表格可能把 T 替换为空格：重新补回 T，
      // 避免 "2026-09-01 12:00:00" 被浏览器当成 UTC 日期串（仅 date-only 串才 UTC），
      // 实际上带空格的 time 串各浏览器行为不统一，所以一律规范化
      const normalized = /^\d{4}-\d{2}-\d{2} /.test(raw) ? raw.replace(' ', 'T') : raw
      d = new Date(normalized)
    } else {
      d = new Date(raw)
    }
    return isNaN(d.getTime()) ? null : d
  } catch (_) {
    return null
  }
}

/**
 * 计算作业展示状态（修复后的正确逻辑）
 *
 * 优先级：
 *  1. 用户刚提交成功，assignment.status 被直接设为 '已提交'（本次会话立即可见）
 *  2. assignment.evaluated 为 true（从后端加载或评价完成后标记）→ '已提交'
 *  3. 当前时间 > deadline → '已截止'
 *  4. 其余（包括 deadline 缺失/解析失败）→ '进行中'（宽松兜底，避免误拦截学生正常提交）
 *
 * Bug 根因（旧实现）：第一行 `if (!assignment.status) return '已截止'`，
 * 但后端 Assignment 实体没有 status 字段（只有 active），导致所有作业短路返回"已截止"，
 * deadline 比较代码从未执行——无论截止时间设多晚都是已截止。
 */
const getAssignmentStatus = (assignment) => {
  if (!assignment) return '进行中'
  if (assignment.status === '已提交' || assignment.evaluated === true) {
    return '已提交'
  }
  const deadline = parseDeadline(assignment.deadline)
  if (!deadline) return '进行中'
  return new Date() > deadline ? '已截止' : '进行中'
}

onMounted(() => {
  loadCourses()
  loadAssignments()
  
  const courseId = route.query.courseId
  if (courseId) {
    selectedCourse.value = courseId
  }
  
  const userStr = sessionStorage.getItem('user')
  if (userStr) {
    currentUser.value = JSON.parse(userStr)
  }
})

const filteredAssignments = computed(() => {
  let result = assignments.value
  if (selectedCourse.value) {
    result = result.filter(a => a.courseId === parseInt(selectedCourse.value))
  }
  if (statusFilter.value) {
    result = result.filter(a => a.status === statusFilter.value)
  }
  return result
})

const handleLogout = async () => {
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

const getStatusClass = (status) => {
  if (status === '已截止') return 'status-danger'
  if (status === '已提交') return 'status-success'
  return 'status-warning'
}

const formatDeadline = (deadline) => {
  if (!deadline) return '-'
  return deadline.replace('T', ' ')
}

const showToast = (message) => {
  toastMessage.value = message
  showSuccessToast.value = true
  setTimeout(() => {
    showSuccessToast.value = false
  }, 3000)
}

/**
 * 提交成功后通用处理：保存 submissionId、更新 UI 状态，并开始评价轮询。
 * GitHub 仓库提交（/submissions/github）后端返回
 * payload：{ id, evaluationStatus, githubUrl?, githubBranch?, githubInfo? }
 */
const afterSubmitOk = (assignment, payload, successMessage) => {
  assignment.status = '已提交'
  assignment.submittedTime = new Date().toLocaleString('zh-CN')
  assignment.submissionCount++
  assignment.evaluated = false
  assignment.submissionId = payload?.id ?? null
  // GitHub 模式回填规范化仓库 URL 与实际分支
  if (payload?.githubUrl) {
    assignment.githubUrl = payload.githubUrl
    if (payload.githubBranch) assignment.githubBranch = payload.githubBranch
  }
  if (payload?.githubInfo) {
    assignment.githubInfo = payload.githubInfo
  }
  ElMessage.success(successMessage)
  // 立即开始评价轮询：后端已受理（202），evaluationStatus 通常为 PENDING
  startPollingFromStatus(assignment, payload?.evaluationStatus)
}

/**
 * 从后端返回的 evaluationStatus 决定是直接走轮询还是命中 COMPLETED 就回填结果。
 * 与 triggerEvaluation 方法的后半段保持一致，避免重复逻辑。
 */
const startPollingFromStatus = (assignment, evalStatus) => {
  if (!evalStatus || !assignment.submissionId) return
  if (evalStatus.status === 'COMPLETED') {
    applyEvaluationCompleted(assignment, evalStatus)
    return
  }
  assignment.evaluationStatus = evalStatus.status || 'PENDING'
  assignment.evaluationError = evalStatus.errorMessage || ''
  startPolling(assignment.submissionId, {
    onUpdate: (data) => { assignment.evaluationStatus = data.status },
    onCompleted: (data) => applyEvaluationCompleted(assignment, data),
    onFailed: (msg) => {
      assignment.evaluationStatus = 'FAILED'
      assignment.evaluationError = msg
      ElMessage.error(`作业 "${assignment.title}" 评价失败：${msg}`)
    }
  })
}

/**
 * 【GitHub 仓库提交】调 POST /api/submissions/github。
 * 后端会下载 zipball、解压、合并源代码文件、落库、并异步触发评价。
 * 失败（URL 格式错误 / 限流 / 仓库私有 / 分支不存在 / 无代码文件）返回 400 + 中文友好错误，
 * 由 ElMessage 直接提示学生，学生可修改后重新提交。
 */
const submitGithub = async (assignment) => {
  if (assignment.submitting) return
  const url = (assignment.githubUrl || assignment.gitUrl || '').trim()
  if (!url) {
    ElMessage.error('请填写 GitHub 仓库地址，例如 https://github.com/user/repo')
    return
  }
  assignment.submitting = true
  try {
    const submitData = {
      assignmentId: assignment.id,
      githubUrl: url,
      githubBranch: (assignment.branch && assignment.branch.trim()) || undefined,
      commitMessage: assignment.commitMessage || undefined
    }
    const response = await submissionApi.submitGithub(submitData)
    if (response?.success) {
      const info = response.data?.githubInfo
      const msg = info
        ? `提交成功！已从 ${info.owner}/${info.repo}@${info.branch} 抽取 ${info.fileCount} 个代码文件，评价任务已受理`
        : `作业 "${assignment.title}" GitHub 提交成功，评价任务已受理`
      afterSubmitOk(assignment, response.data, msg)
    } else {
      ElMessage.error('GitHub 提交失败: ' + (response?.message || '未知错误'))
    }
  } catch (error) {
    console.error('GitHub 提交失败:', error)
    // axios 响应拦截器已经过包装，错误对象通常有 response.data.message 可用
    const detail = error?.response?.data?.message || error?.message || '未知错误'
    ElMessage.error('GitHub 提交失败：' + detail)
  } finally {
    assignment.submitting = false
  }
}

/**
 * GitHub URL 失焦规范化：去掉末尾 .git / 末尾斜杠，避免用户拷贝 clone 地址后仍能正确识别。
 * 不抛错、不弹窗，规范化后写回 githubUrl 字段。
 */
const normalizeGithubInput = (assignment) => {
  let url = (assignment.githubUrl || assignment.gitUrl || '').trim()
  if (!url) return
  if (url.endsWith('.git')) url = url.slice(0, -'.git'.length)
  if (url.endsWith('/')) url = url.slice(0, -1)
  assignment.githubUrl = url
  assignment.gitUrl = url
}

// ==================== Phase 2：AI 评价异步链路 ====================
// 旧版 evaluateSubmission（同步调 analysisApi + 失败硬编码 85 分假数据）已删除，
// 改为"受理 + 轮询"模式：提交端点直接在后端同步受理评价并在 202 返回 evaluationStatus；
// 前端在 afterSubmitOk 中据此开启轮询；详情页"查看评价"按钮复用 GET /evaluations/status。

const triggerEvaluation = async (assignment) => {
  if (!assignment.submissionId) {
    ElMessage.error('缺少提交记录ID，无法发起 AI 评价')
    return
  }

  // 立即进入"排队中"状态，驱动模板展示进度徽标
  assignment.evaluationStatus = 'PENDING'
  assignment.evaluationError = ''

  try {
    // 异步受理：后端立即返回 202 + 当前状态；若幂等命中 COMPLETED 则直接展示结果
    const res = await evaluationApi.evaluate(assignment.submissionId)
    if (res?.data?.status === 'COMPLETED') {
      applyEvaluationCompleted(assignment, res.data)
      return
    }

    // 开启轮询：3s 起步、1.5 倍退避（上限 5s）、最多 60 次判定超时
    startPolling(assignment.submissionId, {
      onUpdate: (data) => {
        // PENDING → PROCESSING 等中间状态变化，实时刷新徽标文案
        assignment.evaluationStatus = data.status
      },
      onCompleted: (data) => applyEvaluationCompleted(assignment, data),
      onFailed: (msg) => {
        assignment.evaluationStatus = 'FAILED'
        assignment.evaluationError = msg
        ElMessage.error(`作业 "${assignment.title}" 评价失败：${msg}`)
      }
    })
  } catch (error) {
    console.error('发起 AI 评价失败:', error)
    assignment.evaluationStatus = 'FAILED'
    assignment.evaluationError = '发起评价请求失败'
    ElMessage.error('发起 AI 评价失败，请检查网络后重试')
  }
}

// 轮询收到 COMPLETED：回填真实分数（替代旧版失败硬编码 85 分的假数据）
const applyEvaluationCompleted = (assignment, data) => {
  assignment.evaluationStatus = 'COMPLETED'
  assignment.evaluated = true
  assignment.finalScore = data.finalScore ?? 0
  ElMessage.success(`作业 "${assignment.title}" AI 评价完成！`)
}

// FAILED 状态的重试入口：重新走受理接口（后端幂等逻辑会把 FAILED 记录重置为 PENDING 重新执行）
const retryEvaluation = async (assignment) => {
  assignment.retrying = true
  try {
    await triggerEvaluation(assignment)
  } finally {
    assignment.retrying = false
  }
}

const resubmitAssignment = (assignment) => {
  assignment.status = '进行中'
  assignment.submittedTime = null
  assignment.evaluated = false
  // 重置评价状态：重新提交后会基于新的提交记录再次发起评价
  assignment.evaluationStatus = null
  assignment.evaluationError = ''
  assignment.githubInfo = null
  showToast(`请重新提交作业 "${assignment.title}"`)
}

const viewEvaluation = (assignment) => {
  router.push(`/student/results`)
}
</script>

<style scoped>
.submissions-container {
  display: flex;
  min-height: 100vh;
  background: #f5f5f5;
}

/* sidebar / header 样式已迁移至 AppLayout.vue */

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.native-select {
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  min-width: 150px;
}

.assignments-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.assignment-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.assignment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.assignment-header .assignment-info h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

.assignment-header .assignment-info p {
  margin: 5px 0 0;
  color: #666;
  font-size: 14px;
}

.status-danger {
  color: #ff4d4f;
  padding: 4px 12px;
  background: #fff2f0;
  border-radius: 4px;
  font-size: 14px;
}

.status-success {
  color: #52c41a;
  padding: 4px 12px;
  background: #f6ffed;
  border-radius: 4px;
  font-size: 14px;
}

.status-warning {
  color: #faad14;
  padding: 4px 12px;
  background: #fffbe6;
  border-radius: 4px;
  font-size: 14px;
}

.assignment-details {
  display: flex;
  gap: 24px;
  margin-bottom: 12px;
}

.assignment-details .detail-item {
  color: #666;
  font-size: 14px;
}

.assignment-description {
  margin: 0 0 20px;
  color: #666;
  line-height: 1.6;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 8px;
}

.submit-section {
  border-top: 1px solid #eee;
  padding-top: 20px;
}

.git-submit-form {
  background: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
}

/* el-input / el-textarea 撑满表单容器，并保留原等宽字体观感 */
.form-group :deep(.el-input),
.form-group :deep(.el-textarea) {
  width: 100%;
}

.form-group :deep(.el-input__inner),
.form-group :deep(.el-textarea__inner) {
  font-family: monospace;
}

.git-status-info {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  margin-bottom: 16px;
}

.git-status-info .status-icon {
  font-size: 20px;
}

.git-status-info .icon-success {
  color: #52c41a;
}

.git-status-info .status-text p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.git-status-info .status-text strong {
  color: #333;
}

.submit-actions {
  display: flex;
  justify-content: flex-end;
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

.warning-btn {
  padding: 10px 20px;
  background: #faad14;
  color: white;
  border: none;
  cursor: pointer;
  border-radius: 4px;
}

.warning-btn:hover {
  background: #ffc53d;
}

.submitted-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #f0fdf4;
  border-radius: 8px;
}

.submitted-status {
  display: flex;
  gap: 12px;
}

.submitted-status .status-icon {
  font-size: 24px;
  color: #52c41a;
}

.submitted-status .status-details p {
  margin: 0 0 4px;
  font-size: 14px;
  color: #666;
}

.submitted-status .status-details p:first-child {
  color: #333;
}

.evaluation-section {
  margin-top: 20px;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.eval-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.eval-badge {
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 12px;
  border-radius: 4px;
  color: white;
  font-size: 14px;
}

.eval-score {
  color: white;
  font-size: 18px;
  font-weight: bold;
}

.action-btn {
  padding: 8px 16px;
  background: white;
  color: #667eea;
  border: none;
  cursor: pointer;
  border-radius: 4px;
  font-size: 14px;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.9);
}

/* ===== Phase 2：评价状态反馈样式 ===== */
.eval-progress {
  margin-top: 12px;
  width: 100%;
}

/* Element Plus 组件内部元素在 scoped 样式中需要 :deep() 穿透 */
.evaluation-section :deep(.el-progress-bar__outer) {
  background: rgba(255, 255, 255, 0.25);
}

.evaluation-section :deep(.el-progress-bar__inner) {
  background: white;
}

/* 评价中徽标的呼吸圆点动画 */
.loading-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 6px;
  border-radius: 50%;
  background: white;
  animation: evalPulse 1s infinite ease-in-out;
}

@keyframes evalPulse {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
}

/* 失败态：红色渐变底 + 徽标加深，与完成态的紫色渐变区分 */
.evaluation-failed {
  background: linear-gradient(135deg, #f56c6c 0%, #c45656 100%);
}

.eval-badge-failed {
  background: rgba(0, 0, 0, 0.15);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state p {
  margin: 0 0 8px;
  color: #666;
}

.empty-state .empty-hint {
  color: #888;
  font-size: 14px;
}

.success-toast {
  position: fixed;
  bottom: 30px;
  right: 30px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 24px;
  background: #52c41a;
  color: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  animation: slideIn 0.3s ease;
}

.success-toast .toast-icon {
  font-size: 20px;
}

.success-toast .toast-message {
  font-size: 14px;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
</style>