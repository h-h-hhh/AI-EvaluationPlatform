<template>
  <!--
    可复用 ECharts 图表容器（Phase 4）
    使用 echarts/core 按需注册（RadarChart + BarChart + CanvasRenderer），
    相比全量引入 echarts 可显著减小打包体积。

    用法：
      <BaseChart :option="radarOption" height="300px" />

    - option 变化时自动 setOption({ notMerge: true }) 整体替换配置
    - window resize 时自动 chart.resize() 保持自适应
    - 组件卸载时 dispose + 移除监听，防止内存泄漏
  -->
  <div ref="chartRef" class="base-chart" :style="{ width: '100%', height: height }"></div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
// 按需引入 echarts 核心：只注册项目用到的图表类型与组件
import * as echarts from 'echarts/core'
import { RadarChart, BarChart } from 'echarts/charts'
import {
  GridComponent,      // 柱状图直角坐标系
  TooltipComponent,   // 悬浮提示
  LegendComponent,    // 图例（分布图后续加对比系列时可用）
  RadarComponent      // 雷达图坐标系（indicator 定义）
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 注册图表类型与组件（模块级执行一次即可，多个 BaseChart 实例共享）
echarts.use([RadarChart, BarChart, GridComponent, TooltipComponent, LegendComponent, RadarComponent, CanvasRenderer])

// props：option 为 ECharts 配置对象（必填）；height 控制容器高度
const props = defineProps({
  option: {
    type: Object,
    required: true
  },
  height: {
    type: String,
    default: '300px'
  }
})

const chartRef = ref(null)
let chartInstance = null

/**
 * window resize 回调：图表容器随窗口缩放自适应
 * 存为具名函数以便卸载时精确移除监听
 */
const handleResize = () => {
  chartInstance?.resize()
}

onMounted(() => {
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption(props.option)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  // dispose 释放 canvas 实例，防止 SPA 路由切换后内存泄漏
  chartInstance?.dispose()
  chartInstance = null
})

// option 深度监听：父组件数据异步加载完成后更新图表。
// notMerge: true 整体替换配置，避免旧 series 残留（例如数据从有到无时柱子不清空）
watch(
  () => props.option,
  (newOption) => {
    if (chartInstance && newOption) {
      chartInstance.setOption(newOption, { notMerge: true })
    }
  },
  { deep: true }
)
</script>

<style scoped>
.base-chart {
  min-width: 0; /* 防止 flex/grid 容器内图表撑破布局 */
}
</style>
