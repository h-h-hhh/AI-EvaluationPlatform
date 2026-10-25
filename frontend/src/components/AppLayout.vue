<template>
  <!--
    多根节点布局：aside + main 直接作为根元素。
    父组件的 .xxx-container（display: flex）直接包裹这两个根元素，维持左右分栏。
    不包一层 div，避免破坏 flex 布局层级。
  -->
  <aside class="sidebar" :style="{ background: sidebarGradient }">
    <div class="logo">
      <h2>代码评价平台</h2>
    </div>
    <nav class="sidebar-nav">
      <!-- 菜单项：div + @click 跳转（与原页面一致，不用 router-link） -->
      <div
        v-for="item in menu"
        :key="item.path"
        :class="['nav-item', { active: item.path === activePath }]"
        @click="$router.push(item.path)"
      >
        {{ item.label }}
      </div>
    </nav>
    <!-- 退出登录：触发 emit，由父组件执行 handleLogout 逻辑 -->
    <div class="logout-btn" @click="$emit('logout')">
      退出登录
    </div>
  </aside>

  <main class="main-content" :class="{ 'main-content-scroll': contentScroll }">
    <!-- header：当父组件传 #header-actions slot 时，加 flex 布局让标题和右侧操作区左右分布 -->
    <header class="header" :class="{ 'header-with-actions': $slots['header-actions'] }">
      <div>
        <h1>{{ title }}</h1>
        <!-- 副标题非必填：未传或传空串时不渲染空 <p>，避免多占 5px 边距 -->
        <p v-if="subtitle">{{ subtitle }}</p>
      </div>
      <slot name="header-actions" />
    </header>
    <!-- 默认 slot：页面主内容区（filter-bar、列表、卡片等） -->
    <slot />
  </main>
</template>

<script setup>
/**
 * AppLayout - 公共布局组件
 * 提供 sidebar（logo + 菜单 + 退出登录）和 header（标题 + 副标题 + 可选右侧操作区）
 * 多根节点设计，aside 和 main 直接作为父组件 flex 容器的子元素
 */
defineProps({
  /** 菜单项数组：[{ label: string, path: string }] */
  menu: { type: Array, required: true },
  /** 当前 active 菜单项的 path，用于动态高亮 */
  activePath: { type: String, required: true },
  /** header 标题 */
  title: { type: String, required: true },
  /** header 副标题（非必填：不传则不渲染，不占位） */
  subtitle: { type: String, default: '' },
  /**
   * sidebar 背景渐变。默认值为学生/教师端原有的蓝色，
   * 保证已迁移页面零影响；角色配色不同的页面（深色 slate / 管理端紫）自行覆盖。
   * 用内联 style 绑定而非 SCSS 变量插值，避免渐变中的 # 被 sass 解析。
   */
  sidebarGradient: {
    type: String,
    default: 'linear-gradient(180deg, #1a73e8 0%, #0d47a1 100%)'
  },
  /**
   * 主内容区是否由内部滚动（overflow: auto）。
   * 基线差异：Dashboard / 管理端共 6 页原本带 overflow: auto，学生与教师端 6 个列表页原本不带。
   * 默认 false 保持列表页现状（对已迁移页面零回溯改动），需要的页面自行声明。
   */
  contentScroll: {
    type: Boolean,
    default: false
  }
})

defineEmits(['logout'])
</script>

<style lang="scss" scoped>
/* sidebar：$sidebar-width 固定宽度，纵向 flex 布局。
   背景渐变由 sidebarGradient prop 以内联 style 提供（见模板），此处不再硬编码。 */
.sidebar {
  width: $sidebar-width;
  color: white;
  display: flex;
  flex-direction: column;
}

/* logo 区域 */
.logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo h2 {
  margin: 0;
  font-size: 18px;
}

/* 菜单导航区：flex: 1 撑满 sidebar 中部空间 */
.sidebar-nav {
  flex: 1;
  padding: 10px 0;
}

/* 菜单项：padding + cursor + hover/active 背景色 */
.nav-item {
  padding: 12px 20px;
  cursor: pointer;
  transition: background 0.2s;
}

.nav-item:hover,
.nav-item.active {
  background: rgba(255, 255, 255, 0.1);
}

/* 退出登录按钮：固定在 sidebar 底部 */
.logout-btn {
  padding: 15px 20px;
  cursor: pointer;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

/* 主内容区：flex: 1 占据剩余宽度 */
.main-content {
  flex: 1;
  padding: 20px;
}

/* 需内部滚动的页面（contentScroll=true）才加 overflow: auto。
   基线：Dashboard / 管理端原本带，学生与教师端列表页原本不带。 */
.main-content-scroll {
  overflow: auto;
}

/* header：默认仅 margin-bottom；当有 header-actions slot 时加 flex 左右分布 */
.header {
  margin-bottom: 20px;
}

.header-with-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
</style>
