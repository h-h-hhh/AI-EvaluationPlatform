/**
 * 评价状态轮询 composable
 *
 * 配合后端"异步受理 + 轮询"评价链路使用（Phase 1 的 POST /evaluations/{submissionId}
 * 秒级受理 + GET /evaluations/status/{submissionId} 状态查询）。
 *
 * ================================ 设计思路 ================================
 *
 * 1. 如何避免组件卸载后继续轮询？
 *    - 双保险机制：
 *      a) onUnmounted(stop)：组件卸载时立即清除定时器并将 active 置为 false；
 *      b) 软停止标记 active：scheduleNext/pollOnce 入口处检查 active，
 *         即使卸载发生在 await 请求挂起期间，请求返回后也不会再排下一轮。
 *    - 使用 setTimeout 链而非 setInterval：每次请求完成（或失败）后才排下一轮，
 *       避免请求堆积；同时链式调用让每一轮都能感知最新的 active 状态。
 *
 * 2. 如何判定超时？
 *    - 按"轮询次数上限"（maxAttempts，默认 60 次）判定而非墙钟时间：
 *      浏览器对后台标签页的定时器有节流（最小间隔可达 1s 甚至更长），
 *      按时间判定会误杀；按次数判定则每轮必然发生一次网络请求，更可靠。
 *    - 默认配置下总耗时约 3~5 分钟（含退避），覆盖 DeepSeek 10~120s 的真实耗时区间。
 *    - 达到上限 → stop() 并回调 onFailed('评价超时')，页面展示重试按钮。
 *
 * 3. 如何支持手动重试？
 *    - 暴露 retry(submissionId, callbacks)：内部等价于 stop() + start()，
 *      重置计数器与退避间隔后重新开始轮询。
 *    - 注意：触发评价的 POST 请求由页面负责（本 composable 只管轮询）；
 *      后端幂等设计保证 FAILED 记录重新 POST 后会被重置为 PENDING 重新执行。
 *
 * 4. 退避策略（不要太夸张）：
 *    - 间隔从 interval（3s）起步，每轮 × backoffFactor（1.5），封顶 maxInterval（5s）。
 *      即 3s → 4.5s → 5s → 5s…，既减少无效请求又不让用户等待感明显增加。
 *
 * 5. 并发保护：
 *    - start() 内部先 stop()，防止同一页面重复 start 造成双定时器竞态。
 *    - 限制：单实例同一时间只跟踪一个 submissionId 的轮询（学生通常一次评价
 *      一份作业）；如需并发跟踪多个，可多次调用 useEvaluationPolling() 创建实例。
 *
 * 6. 异常容忍：
 *    - 单次轮询网络失败不立即终止（可能是抖动），走退避继续；
 *      连续失败最终由 maxAttempts 兜底结束，不会无限轮询。
 */
import { ref, onUnmounted } from 'vue'
import { evaluationApi } from '../services/api'

export function useEvaluationPolling(options = {}) {
  // ===== 可调参数（页面可通过 options 覆盖） =====
  const {
    interval = 3000,      // 初始轮询间隔（ms）
    backoffFactor = 1.5,  // 退避系数：每轮间隔乘以该值
    maxInterval = 5000,   // 间隔上限（ms），退避不要太夸张
    maxAttempts = 60      // 最大轮询次数，超过判定为超时
  } = options

  // ===== 响应式状态（供页面驱动 UI） =====
  const polling = ref(false)   // 是否正在轮询（可用于按钮 loading）
  const status = ref(null)     // 最近一次轮询拿到的状态DTO {status, finalScore, errorMessage...}

  // ===== 内部可变状态（非响应式，避免额外开销） =====
  let timer = null             // setTimeout 句柄
  let attempt = 0              // 已轮询次数
  let currentInterval = interval // 当前间隔（退避用）
  let active = false           // 软停止标记：false 后所有后续轮次直接短路

  /** 清除待执行的定时器 */
  const clearTimer = () => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  /** 停止轮询：清定时器 + 关闭软停止标记 + 复位 polling 状态 */
  const stop = () => {
    active = false
    clearTimer()
    polling.value = false
  }

  /**
   * 安排下一轮轮询
   * @param {number} submissionId 提交记录ID
   * @param {object} callbacks    页面回调 { onUpdate, onCompleted, onFailed }
   */
  const scheduleNext = (submissionId, callbacks) => {
    // 软停止检查：组件已卸载/已手动停止时不再排下一轮
    if (!active) return

    attempt += 1
    // 超时判定：按次数而非墙钟时间（原因见文件头设计思路第 2 点）
    if (attempt > maxAttempts) {
      stop()
      callbacks?.onFailed?.('评价超时，请稍后重试')
      return
    }

    // setTimeout 链：本轮请求彻底结束后才排下一轮，避免请求堆积
    timer = setTimeout(() => pollOnce(submissionId, callbacks), currentInterval)
    // 指数退避：3s → 4.5s → 5s（封顶），仅影响后续轮次
    currentInterval = Math.min(currentInterval * backoffFactor, maxInterval)
  }

  /**
   * 执行一轮状态查询
   * @param {number} submissionId 提交记录ID
   * @param {object} callbacks    页面回调
   */
  const pollOnce = async (submissionId, callbacks) => {
    // 软停止检查：定时器触发时组件可能已卸载
    if (!active) return

    try {
      // 全局响应拦截器已返回 ApiResponse 本体（{success, message, data}）
      const res = await evaluationApi.getStatus(submissionId)
      const data = res?.data || null
      status.value = data

      if (!data) {
        // 尚无评价记录（受理请求已成功但记录查询为空，理论上罕见）→ 继续等待
        scheduleNext(submissionId, callbacks)
        return
      }

      if (data.status === 'COMPLETED') {
        // 终态：完成 → 停止轮询并回调页面回填分数
        stop()
        callbacks?.onCompleted?.(data)
        return
      }

      if (data.status === 'FAILED') {
        // 终态：失败 → 停止轮询并回调页面展示错误与重试按钮
        stop()
        callbacks?.onFailed?.(data.errorMessage || '评价失败')
        return
      }

      // 中间态（PENDING / PROCESSING）→ 通知页面更新徽标，继续下一轮
      callbacks?.onUpdate?.(data)
      scheduleNext(submissionId, callbacks)
    } catch (error) {
      // 网络抖动容错：不立即终止，走退避继续；maxAttempts 兜底防止无限轮询
      console.error('轮询评价状态失败:', error)
      scheduleNext(submissionId, callbacks)
    }
  }

  /**
   * 开始轮询
   * @param {number} submissionId 提交记录ID
   * @param {object} callbacks    { onUpdate(data), onCompleted(data), onFailed(msg) }
   */
  const start = (submissionId, callbacks = {}) => {
    stop()                         // 并发保护：先停掉可能存在的旧轮询，防止双定时器
    attempt = 0                    // 重置计数
    currentInterval = interval     // 重置退避间隔
    active = true
    polling.value = true
    scheduleNext(submissionId, callbacks)
  }

  /**
   * 手动重试：重置内部状态后重新开始轮询
   * （触发评价的 POST 请求由页面负责，本方法只负责重启轮询）
   */
  const retry = (submissionId, callbacks = {}) => {
    start(submissionId, callbacks)
  }

  // 组件卸载时自动清理，防止"离开页面后仍在后台轮询"
  onUnmounted(stop)

  return { polling, status, start, stop, retry }
}
