<template>
  <div class="results-container">
    <aside class="sidebar">
      <div class="logo">
        <h2>代码评价平台</h2>
      </div>
      <nav class="sidebar-nav">
        <div class="nav-item" @click="$router.push('/student')">
          学习看板
        </div>
        <div class="nav-item" @click="$router.push('/student/courses')">
          我的课程
        </div>
        <div class="nav-item" @click="$router.push('/student/submissions')">
          作业提交
        </div>
        <div class="nav-item active" @click="$router.push('/student/results')">
          评价结果
        </div>
      </nav>
      <div class="logout-btn" @click="handleLogout">
        退出登录
      </div>
    </aside>
    
    <main class="main-content">
      <header class="header">
        <div>
          <h1>评价结果</h1>
          <p>查看您的代码作业评价详情</p>
        </div>
      </header>
      
      <div class="stats-overview">
        <div class="stat-card">
          <div class="stat-icon">📝</div>
          <div class="stat-info">
            <p class="stat-value">{{ totalSubmissions }}</p>
            <p class="stat-label">总提交数</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">✅</div>
          <div class="stat-info">
            <p class="stat-value">{{ evaluatedCount }}</p>
            <p class="stat-label">已评价</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">📊</div>
          <div class="stat-info">
            <p class="stat-value">{{ averageScore }}</p>
            <p class="stat-label">平均分</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🏆</div>
          <div class="stat-info">
            <p class="stat-value">{{ highestScore }}</p>
            <p class="stat-label">最高分</p>
          </div>
        </div>
      </div>
      
      <div class="filter-bar">
        <select v-model="selectedCourse" class="native-select">
          <option value="">全部课程</option>
          <option v-for="course in courses" :key="course.id" :value="course.id">
            {{ course.name }}
          </option>
        </select>
        <select v-model="statusFilter" class="native-select">
          <option value="">全部状态</option>
          <option value="已评价">已评价</option>
          <option value="待评价">待评价</option>
        </select>
      </div>
      
      <div class="results-list">
        <div v-for="result in filteredResults" :key="result.id" class="result-card">
          <div class="result-header">
            <div class="result-info">
              <h3>{{ result.assignmentTitle }}</h3>
              <p>{{ result.courseName }} - {{ result.submittedTime }}</p>
            </div>
            <div class="score-badge" :class="getScoreClass(result.finalScore)">
              {{ result.finalScore }}分
            </div>
          </div>
          
          <div class="scores-grid">
            <div class="score-item">
              <div class="score-label">正确性</div>
              <div class="score-bar">
                <div class="score-fill" :style="{ width: result.scores.correctness + '%' }"></div>
              </div>
              <div class="score-value">{{ result.scores.correctness }}</div>
            </div>
            <div class="score-item">
              <div class="score-label">代码质量</div>
              <div class="score-bar">
                <div class="score-fill" :style="{ width: result.scores.quality + '%' }"></div>
              </div>
              <div class="score-value">{{ result.scores.quality }}</div>
            </div>
            <div class="score-item">
              <div class="score-label">性能优化</div>
              <div class="score-bar">
                <div class="score-fill" :style="{ width: result.scores.performance + '%' }"></div>
              </div>
              <div class="score-value">{{ result.scores.performance }}</div>
            </div>
            <div class="score-item">
              <div class="score-label">安全性</div>
              <div class="score-bar">
                <div class="score-fill" :style="{ width: result.scores.security + '%' }"></div>
              </div>
              <div class="score-value">{{ result.scores.security }}</div>
            </div>
            <div class="score-item">
              <div class="score-label">创新点</div>
              <div class="score-bar">
                <div class="score-fill" :style="{ width: result.scores.originality + '%' }"></div>
              </div>
              <div class="score-value">{{ result.scores.originality }}</div>
            </div>
          </div>
          
          <div class="summary-section">
            <h4>综合评价</h4>
            <p>{{ result.summary }}</p>
          </div>
          
          <div class="suggestions-section">
            <h4>改进建议</h4>
            <ul>
              <li v-for="(suggestion, index) in result.suggestions" :key="index">
                <span class="suggestion-num">{{ index + 1 }}</span>
                {{ suggestion }}
              </li>
            </ul>
          </div>
          
          <div class="detail-sections">
            <div class="detail-section">
              <h4>正确性评价</h4>
              <p>{{ result.details.correctness.comment }}</p>
            </div>
            <div class="detail-section">
              <h4>代码质量评价</h4>
              <p>{{ result.details.quality.comment }}</p>
            </div>
            <div class="detail-section">
              <h4>性能评价</h4>
              <p>{{ result.details.performance.comment }}</p>
            </div>
            <div class="detail-section">
              <h4>安全性评价</h4>
              <p>{{ result.details.security.comment }}</p>
            </div>
            <div class="detail-section">
              <h4>创新性评价</h4>
              <p>{{ result.details.originality.comment }}</p>
            </div>
          </div>
          
          <div class="result-actions">
            <button class="action-btn" @click="viewCode(result)">查看代码</button>
            <button class="action-btn" @click="resubmit(result)">重新提交</button>
          </div>
        </div>
        
        <div v-if="filteredResults.length === 0" class="empty-state">
          <div class="empty-icon">📊</div>
          <p>暂无评价结果</p>
          <p class="empty-hint">提交作业后将自动进行评价</p>
        </div>
      </div>
    </main>
    
    <div v-if="showCodeModal" class="modal-overlay" @click="showCodeModal = false">
      <div class="code-modal" @click.stop>
        <div class="modal-header">
          <h3>提交代码</h3>
          <button class="close-btn" @click="showCodeModal = false">×</button>
        </div>
        <div class="code-content">
          <pre><code>{{ currentCode }}</code></pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { submissionApi, courseApi } from '../../services/api'

const router = useRouter()

const selectedCourse = ref('')
const statusFilter = ref('')
const showCodeModal = ref(false)
const currentCode = ref('')

const courses = ref([])
const results = ref([])

const loadCourses = async () => {
  try {
    const response = await courseApi.getEnrolled()
    if (response.success && response.data) {
      courses.value = response.data.map(c => ({ id: c.id, name: c.name }))
    }
  } catch (error) {
    console.error('加载课程失败:', error)
  }
}

const loadResults = async () => {
  try {
    const response = await submissionApi.getByStudent()
    if (response.success && response.data) {
      results.value = response.data.map(r => {
        const evaluation = r.evaluation || {}
        const scores = evaluation.scores || {
          correctness: 0,
          quality: 0,
          performance: 0,
          security: 0,
          originality: 0
        }
        
        return {
          id: r.id,
          assignmentTitle: r.assignment?.title || '未知作业',
          courseName: r.assignment?.course?.name || '',
          submittedTime: r.submittedAt || '-',
          finalScore: scores.final || calculateFinalScore(scores),
          scores: scores,
          summary: evaluation.summary || '暂无评价',
          suggestions: evaluation.suggestions || [],
          details: {
            correctness: evaluation.correctness || { comment: '-' },
            quality: evaluation.quality || { comment: '-' },
            performance: evaluation.performance || { comment: '-' },
            security: evaluation.security || { comment: '-' },
            originality: evaluation.originality || { comment: '-' }
          },
          codeContent: r.codeContent || '',
          status: evaluation ? '已评价' : '待评价'
        }
      })
    } else {
      results.value = generateMockResults()
    }
  } catch (error) {
    console.error('加载评价结果失败:', error)
    results.value = generateMockResults()
  }
}

const calculateFinalScore = (scores) => {
  const total = scores.correctness * 0.3 + 
                scores.quality * 0.25 + 
                scores.performance * 0.2 + 
                scores.security * 0.15 + 
                scores.originality * 0.1
  return Math.round(total)
}

const generateMockResults = () => {
  return [
    {
      id: 1,
      assignmentTitle: 'Java基础练习 - 数组排序',
      courseName: 'Java程序设计',
      submittedTime: '2024-01-15 14:30',
      finalScore: 88,
      scores: {
        correctness: 95,
        quality: 85,
        performance: 80,
        security: 90,
        originality: 80
      },
      summary: '代码整体质量良好，算法实现正确。代码结构清晰，命名规范。建议在性能优化方面进一步改进，可以考虑使用更高效的排序算法。',
      suggestions: [
        '考虑使用快速排序或归并排序代替冒泡排序',
        '增加代码注释，提高可读性',
        '添加异常处理机制',
        '考虑代码复用性，抽取公共方法',
        '使用泛型提高代码通用性'
      ],
      details: {
        correctness: { comment: '代码逻辑正确，能够正确完成数组排序功能，边界条件处理完善。' },
        quality: { comment: '代码风格良好，命名规范清晰。建议增加更多注释说明算法思路。' },
        performance: { comment: '当前使用冒泡排序，时间复杂度为O(n²)，对于大数据量效率较低。' },
        security: { comment: '代码安全性良好，没有明显的安全漏洞。输入验证完善。' },
        originality: { comment: '算法实现较为常规，建议尝试更创新的解决方案。' }
      },
      codeContent: 'public class SortExample {\n    public static void main(String[] args) {\n        int[] arr = {3, 1, 4, 1, 5, 9, 2, 6};\n        bubbleSort(arr);\n        for (int num : arr) {\n            System.out.print(num + \" \");\n        }\n    }\n    \n    public static void bubbleSort(int[] arr) {\n        for (int i = 0; i < arr.length - 1; i++) {\n            for (int j = 0; j < arr.length - i - 1; j++) {\n                if (arr[j] > arr[j + 1]) {\n                    int temp = arr[j];\n                    arr[j] = arr[j + 1];\n                    arr[j + 1] = temp;\n                }\n            }\n        }\n    }\n}',
      status: '已评价'
    },
    {
      id: 2,
      assignmentTitle: '学生管理系统',
      courseName: 'Java程序设计',
      submittedTime: '2024-01-18 10:20',
      finalScore: 75,
      scores: {
        correctness: 70,
        quality: 75,
        performance: 65,
        security: 80,
        originality: 85
      },
      summary: '功能基本实现，但存在一些问题。创新性较好，设计思路清晰。建议加强代码质量和性能优化。',
      suggestions: [
        '修复学生添加功能的bug',
        '使用数据库连接池',
        '增加日志记录',
        '优化查询算法',
        '添加单元测试'
      ],
      details: {
        correctness: { comment: '基本功能实现，但添加学生时存在空指针异常风险。' },
        quality: { comment: '代码结构较为清晰，但类设计可以进一步优化。' },
        performance: { comment: '查询效率较低，建议使用索引或优化查询语句。' },
        security: { comment: '输入验证完善，SQL注入防护良好。' },
        originality: { comment: '采用了较为新颖的设计模式，值得肯定。' }
      },
      codeContent: 'public class StudentManager {\n    private List<Student> students = new ArrayList<>();\n    \n    public void addStudent(Student student) {\n        if (student != null) {\n            students.add(student);\n        }\n    }\n    \n    public Student findById(int id) {\n        for (Student s : students) {\n            if (s.getId() == id) {\n                return s;\n            }\n        }\n        return null;\n    }\n}',
      status: '已评价'
    },
    {
      id: 3,
      assignmentTitle: '数据结构 - 链表实现',
      courseName: '数据结构与算法',
      submittedTime: '2024-01-20 16:45',
      finalScore: 92,
      scores: {
        correctness: 95,
        quality: 90,
        performance: 95,
        security: 90,
        originality: 85
      },
      summary: '代码质量优秀，算法实现高效。链表的基本操作实现正确，代码结构清晰。',
      suggestions: [
        '考虑实现双向链表',
        '添加更多边界条件检查',
        '实现迭代器接口',
        '考虑并发安全问题',
        '增加泛型支持'
      ],
      details: {
        correctness: { comment: '链表的增删改查操作均正确实现，边界条件处理完善。' },
        quality: { comment: '代码风格优秀，注释充分，可读性强。' },
        performance: { comment: '算法效率高，时间复杂度符合预期。' },
        security: { comment: '代码安全性良好，输入验证完善。' },
        originality: { comment: '实现较为标准，建议尝试更优化的实现方式。' }
      },
      codeContent: 'public class LinkedList<T> {\n    private Node<T> head;\n    \n    private static class Node<T> {\n        T data;\n        Node<T> next;\n        Node(T data) { this.data = data; }\n    }\n    \n    public void add(T data) {\n        Node<T> newNode = new Node<>(data);\n        if (head == null) { head = newNode; }\n        else {\n            Node<T> current = head;\n            while (current.next != null) current = current.next;\n            current.next = newNode;\n        }\n    }\n}',
      status: '已评价'
    }
  ]
}

onMounted(() => {
  loadCourses()
  loadResults()
})

const filteredResults = computed(() => {
  let result = results.value
  if (selectedCourse.value) {
    result = result.filter(r => r.courseName)
  }
  if (statusFilter.value) {
    result = result.filter(r => r.status === statusFilter.value)
  }
  return result
})

const totalSubmissions = computed(() => results.value.length)

const evaluatedCount = computed(() => 
  results.value.filter(r => r.status === '已评价').length
)

const averageScore = computed(() => {
  const evaluated = results.value.filter(r => r.status === '已评价')
  if (evaluated.length === 0) return '-'
  const total = evaluated.reduce((sum, r) => sum + r.finalScore, 0)
  return Math.round(total / evaluated.length)
})

const highestScore = computed(() => {
  const evaluated = results.value.filter(r => r.status === '已评价')
  if (evaluated.length === 0) return '-'
  return Math.max(...evaluated.map(r => r.finalScore))
})

const handleLogout = () => {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('role')
  sessionStorage.removeItem('user')
  router.push('/')
}

const getScoreClass = (score) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 60) return 'score-pass'
  return 'score-fail'
}

const viewCode = (result) => {
  currentCode.value = result.codeContent
  showCodeModal.value = true
}

const resubmit = (result) => {
  router.push(`/student/submissions`)
}
</script>

<style scoped>
.results-container {
  display: flex;
  min-height: 100vh;
  background: #f5f5f5;
}

.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #1a73e8 0%, #0d47a1 100%);
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
  margin-bottom: 20px;
}

.header h1 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.header p {
  margin: 5px 0 0;
  color: #666;
}

.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  font-size: 32px;
}

.stat-info .stat-value {
  margin: 0;
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.stat-info .stat-label {
  margin: 5px 0 0;
  font-size: 14px;
  color: #666;
}

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

.results-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.result-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.result-header .result-info h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

.result-header .result-info p {
  margin: 5px 0 0;
  color: #666;
  font-size: 14px;
}

.score-badge {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 24px;
  font-weight: bold;
}

.score-excellent {
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
  color: white;
}

.score-good {
  background: linear-gradient(135deg, #409eff 0%, #1890ff 100%);
  color: white;
}

.score-pass {
  background: linear-gradient(135deg, #faad14 0%, #fa8c16 100%);
  color: white;
}

.score-fail {
  background: linear-gradient(135deg, #ff4d4f 0%, #cf1322 100%);
  color: white;
}

.scores-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.score-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.score-label {
  font-size: 12px;
  color: #666;
  text-align: center;
}

.score-bar {
  height: 8px;
  background: #eee;
  border-radius: 4px;
  overflow: hidden;
}

.score-fill {
  height: 100%;
  background: linear-gradient(90deg, #409eff 0%, #66b1ff 100%);
  border-radius: 4px;
  transition: width 0.5s ease;
}

.score-value {
  font-size: 14px;
  font-weight: bold;
  text-align: center;
  color: #333;
}

.summary-section {
  background: #f9f9f9;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.summary-section h4 {
  margin: 0 0 8px;
  color: #333;
  font-size: 14px;
}

.summary-section p {
  margin: 0;
  color: #666;
  line-height: 1.6;
}

.suggestions-section {
  margin-bottom: 20px;
}

.suggestions-section h4 {
  margin: 0 0 12px;
  color: #333;
  font-size: 14px;
}

.suggestions-section ul {
  margin: 0;
  padding: 0;
  list-style: none;
}

.suggestions-section li {
  padding: 10px 15px;
  background: #fff7e6;
  margin-bottom: 8px;
  border-radius: 6px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.suggestion-num {
  background: #ffa940;
  color: white;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}

.detail-sections {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.detail-section {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
}

.detail-section h4 {
  margin: 0 0 8px;
  color: #333;
  font-size: 13px;
  font-weight: 600;
}

.detail-section p {
  margin: 0;
  color: #666;
  font-size: 13px;
  line-height: 1.5;
}

.result-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.action-btn {
  padding: 8px 16px;
  border: 1px solid #409eff;
  background: white;
  color: #409eff;
  cursor: pointer;
  border-radius: 4px;
  font-size: 14px;
}

.action-btn:hover {
  background: #e6f7ff;
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

.code-modal {
  background: white;
  border-radius: 12px;
  width: 80%;
  max-height: 80%;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.close-btn:hover {
  color: #333;
}

.code-content {
  padding: 20px;
  overflow: auto;
  max-height: 60vh;
}

.code-content pre {
  margin: 0;
  background: #1e1e1e;
  padding: 20px;
  border-radius: 8px;
  overflow-x: auto;
}

.code-content code {
  color: #d4d4d4;
  font-family: monospace;
  font-size: 14px;
  line-height: 1.6;
}
</style>