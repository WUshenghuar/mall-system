<template>
  <a-layout class="admin-layout">
    <!-- Sidebar -->
    <a-layout-sider
      v-model:collapsed="collapsed"
      :width="240"
      :collapsed-width="64"
      class="admin-sidebar"
      :trigger="null"
      collapsible
    >
      <!-- Brand -->
      <div class="sidebar-brand" :class="{ collapsed }">
        <div class="brand-icon">
          <svg viewBox="0 0 32 32" width="32" height="32">
            <rect width="32" height="32" rx="4" fill="var(--color-brass)"/>
            <text x="16" y="22" text-anchor="middle" fill="var(--color-abyss)" font-family="var(--font-display)" font-weight="700" font-size="16">CB</text>
          </svg>
        </div>
        <transition name="fade-slide">
          <span v-if="!collapsed" class="brand-text">跨境管理</span>
        </transition>
      </div>

      <!-- Navigation -->
      <div class="sidebar-nav">
        <div class="menu-section">
          <transition name="fade-slide">
            <span v-if="!collapsed" class="menu-label">业务管理</span>
          </transition>
          <a-menu
            v-model:selectedKeys="selectedKeys"
            mode="inline"
            class="sidebar-menu"
            @click="handleMenuClick"
          >
            <a-menu-item key="/dashboard" class="menu-item-stagger">
              <template #icon>
                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="4" rx="1"/><rect x="14" y="10" width="7" height="11" rx="1"/><rect x="3" y="13" width="7" height="8" rx="1"/></svg>
              </template>
              <span>工作台</span>
            </a-menu-item>
            <a-menu-item key="/product" class="menu-item-stagger">
              <template #icon>
                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.29 7 12 12 20.71 7"/><line x1="12" y1="22" x2="12" y2="12"/></svg>
              </template>
              <span>商品管理</span>
            </a-menu-item>
            <a-menu-item key="/order" class="menu-item-stagger">
              <template #icon>
                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/><line x1="9" y1="12" x2="15" y2="12"/><line x1="9" y1="16" x2="13" y2="16"/></svg>
              </template>
              <span>订单管理</span>
            </a-menu-item>
            <a-menu-item key="/member" class="menu-item-stagger">
              <template #icon>
                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><polyline points="17 11 19 13 23 9"/></svg>
              </template>
              <span>会员管理</span>
            </a-menu-item>
          </a-menu>
        </div>

        <div class="menu-section">
          <transition name="fade-slide">
            <span v-if="!collapsed" class="menu-label">运营设置</span>
          </transition>
          <a-menu
            v-model:selectedKeys="selectedKeys"
            mode="inline"
            class="sidebar-menu"
            @click="handleMenuClick"
          >
            <a-menu-item key="/marketing" class="menu-item-stagger">
              <template #icon>
                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 12H4"/><path d="M20 12L16 16"/><path d="M20 12L16 8"/><path d="M12 19V5"/><path d="M8 3C5.5 5 4 8 4 12s1.5 7 4 9"/><path d="M16 3c2.5 2 4 5 4 9s-1.5 7-4 9"/></svg>
              </template>
              <span>营销管理</span>
            </a-menu-item>
            <a-menu-item key="/finance" class="menu-item-stagger">
              <template #icon>
                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
              </template>
              <span>财务管理</span>
            </a-menu-item>
            <a-menu-item key="/system" class="menu-item-stagger">
              <template #icon>
                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="3"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>
              </template>
              <span>系统设置</span>
            </a-menu-item>
          </a-menu>
        </div>
      </div>

      <!-- Collapse -->
      <div class="sidebar-footer">
        <div class="sidebar-toggle" @click="collapsed = !collapsed">
          <svg v-if="!collapsed" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="15 18 9 12 15 6"/></svg>
          <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="9 18 15 12 9 6"/></svg>
        </div>
      </div>
    </a-layout-sider>

    <!-- Main area -->
    <a-layout class="admin-main">
      <!-- Header -->
      <a-layout-header class="admin-header">
        <div class="header-left">
          <h1 class="header-title">{{ pageTitle }}</h1>
          <span class="header-breadcrumb">/ {{ route.meta?.module || '' }}</span>
        </div>
        <div class="header-right">
          <!-- Quick status indicators -->
          <div class="header-status">
            <span class="status-indicator live">
              <span class="status-dot"></span>
              <span class="status-text">系统正常</span>
            </span>
          </div>

          <div class="header-divider"></div>

          <!-- User -->
          <a-dropdown placement="bottomRight">
            <div class="user-chip">
              <div class="user-avatar">
                <span>{{ userInitial }}</span>
              </div>
              <span class="user-name">{{ userStore.userInfo?.realName || '管理员' }}</span>
              <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="user-arrow"><polyline points="6 9 12 15 18 9"/></svg>
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="logout" @click="handleLogout" class="logout-item">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="margin-right:8px;vertical-align:middle"><path d="M16 17l5-5-5-5"/><path d="M21 12H9"/><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/></svg>
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <!-- Content -->
      <a-layout-content class="admin-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const collapsed = ref(false)
const selectedKeys = ref(['/dashboard'])

const pageTitle = computed(() => route.meta?.title || '工作台')
const userInitial = computed(() => {
  const name = userStore.userInfo?.realName || '管理员'
  return name.charAt(0)
})

onMounted(() => {
  // Keep sync with route
  selectedKeys.value = [route.path]
})

function handleMenuClick({ key }) {
  router.push(key)
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
/* ════════════════════════════════════════
   ADMIN LAYOUT
   ════════════════════════════════════════ */

.admin-layout {
  height: 100vh;
  overflow: hidden;
}

/* ── Sidebar ── */
.admin-sidebar {
  background: var(--color-abyss) !important;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

/* Subtle diagonal line pattern */
.admin-sidebar::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    repeating-linear-gradient(
      45deg,
      transparent,
      transparent 40px,
      rgba(255, 255, 255, 0.012) 40px,
      rgba(255, 255, 255, 0.012) 41px
    );
  pointer-events: none;
}

/* Brand */
.sidebar-brand {
  height: var(--header-height);
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 0 var(--space-4);
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.brand-icon {
  flex-shrink: 0;
  line-height: 0;
}

.brand-text {
  font-family: var(--font-sans);
  color: #fff;
  font-size: var(--text-lg);
  font-weight: 600;
  letter-spacing: 3px;
  white-space: nowrap;
}

/* Menu sections */
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: var(--space-2) 0;
  position: relative;
  z-index: 1;
}

.menu-section {
  padding: var(--space-1) 0;
}

.menu-section + .menu-section {
  border-top: 1px solid rgba(255, 255, 255, 0.04);
  margin-top: var(--space-2);
  padding-top: var(--space-3);
}

.menu-label {
  display: block;
  padding: 0 var(--space-5);
  margin-bottom: var(--space-1);
  font-size: 10px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.45);
  text-transform: uppercase;
  letter-spacing: 1.8px;
}

/* Menu items */
.sidebar-menu {
  background: transparent !important;
  border-right: none !important;
}

.sidebar-menu .ant-menu-item {
  margin: 2px var(--space-2) !important;
  border-radius: var(--radius-md) !important;
  height: 40px !important;
  line-height: 40px !important;
  color: rgba(255, 255, 255, 0.82) !important;
  font-weight: 400;
  font-size: var(--text-sm) !important;
  transition: all var(--duration-fast) var(--ease-out);
  width: auto !important;
}
.sidebar-menu .ant-menu-item span {
  color: rgba(255, 255, 255, 0.82) !important;
}
.sidebar-menu .ant-menu-item:hover {
  color: #fff !important;
  background: rgba(255, 255, 255, 0.06) !important;
}
.sidebar-menu .ant-menu-item:hover span {
  color: #fff !important;
}
.sidebar-menu .ant-menu-item.ant-menu-item-selected {
  background: var(--color-brass-soft) !important;
  color: var(--color-brass) !important;
  font-weight: 500;
}
.sidebar-menu .ant-menu-item.ant-menu-item-selected span {
  color: var(--color-brass) !important;
}
.sidebar-menu .ant-menu-item.ant-menu-item-selected::before {
  content: '';
  position: absolute;
  left: -4px;
  top: 8px;
  bottom: 8px;
  width: 3px;
  background: var(--color-brass);
  border-radius: 0 3px 3px 0;
  box-shadow: 0 0 8px rgba(200, 150, 62, 0.3);
}
.sidebar-menu .ant-menu-item .anticon {
  font-size: 17px;
}

/* Staggered entry animation */
.menu-item-stagger {
  animation: menu-slide-in 0.5s var(--ease-out) both;
}
.menu-section:first-child .menu-item-stagger:nth-child(1) { animation-delay: 0.05s; }
.menu-section:first-child .menu-item-stagger:nth-child(2) { animation-delay: 0.08s; }
.menu-section:first-child .menu-item-stagger:nth-child(3) { animation-delay: 0.11s; }
.menu-section:first-child .menu-item-stagger:nth-child(4) { animation-delay: 0.14s; }
.menu-section:nth-child(2) .menu-item-stagger:nth-child(1) { animation-delay: 0.18s; }
.menu-section:nth-child(2) .menu-item-stagger:nth-child(2) { animation-delay: 0.21s; }
.menu-section:nth-child(2) .menu-item-stagger:nth-child(3) { animation-delay: 0.24s; }

@keyframes menu-slide-in {
  from { opacity: 0; transform: translateX(-8px); }
  to   { opacity: 1; transform: translateX(0); }
}

/* Footer */
.sidebar-footer {
  margin-top: auto;
  border-top: 1px solid rgba(255, 255, 255, 0.04);
  position: relative;
  z-index: 1;
}

.sidebar-toggle {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.2);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}
.sidebar-toggle:hover {
  color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.03);
}

/* ── Header ── */
.admin-header {
  height: var(--header-height) !important;
  line-height: var(--header-height) !important;
  background: rgba(255, 255, 255, 0.85) !important;
  backdrop-filter: blur(16px) saturate(1.5) !important;
  -webkit-backdrop-filter: blur(16px) saturate(1.5) !important;
  padding: 0 var(--space-6) !important;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(11, 25, 44, 0.04);
  flex-shrink: 0;
  position: relative;
  z-index: 100;
  box-shadow: 0 1px 0 rgba(11, 25, 44, 0.02);
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
}

.header-title {
  font-family: var(--font-sans);
  font-size: var(--text-xl);
  font-weight: 600;
  color: var(--color-lead);
  letter-spacing: -0.2px;
  line-height: 1;
}

.header-breadcrumb {
  font-size: 12px;
  color: var(--color-slate-light);
  letter-spacing: 0.3px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

/* Status indicator */
.header-status {
  display: flex;
  align-items: center;
}

.status-indicator {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-slate);
  letter-spacing: 0.2px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-slate-light);
}

.status-indicator.live .status-dot {
  background: var(--color-success);
  box-shadow: 0 0 6px rgba(45, 138, 86, 0.4);
  animation: dot-pulse 2s ease-in-out infinite;
}

@keyframes dot-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* Divider */
.header-divider {
  width: 1px;
  height: 24px;
  background: rgba(11, 25, 44, 0.06);
}

/* User chip */
.user-chip {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-md);
  transition: all var(--duration-fast) var(--ease-out);
}
.user-chip:hover {
  background: rgba(11, 25, 44, 0.03);
}

.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--color-abyss) 0%, #1a2d4e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--color-brass);
  font-size: 13px;
  font-weight: 600;
  font-family: var(--font-sans);
}

.user-name {
  font-size: var(--text-sm);
  color: var(--color-lead);
  font-weight: 500;
}

.user-arrow {
  color: var(--color-slate-light);
  transition: transform var(--duration-fast) var(--ease-out);
}

.user-chip:hover .user-arrow {
  transform: rotate(180deg);
}

/* ── Content area ── */
.admin-content {
  flex: 1;
  padding: var(--space-6);
  overflow-y: auto;
  background:
    linear-gradient(180deg, var(--color-surface) 0%, var(--color-fog) 40px);
  position: relative;
}

/* Subtle dot pattern in content area corner */
.admin-content::after {
  content: '';
  position: absolute;
  bottom: 0;
  right: 0;
  width: 300px;
  height: 300px;
  background:
    radial-gradient(circle, rgba(200, 150, 62, 0.015) 1px, transparent 1px);
  background-size: 20px 20px;
  pointer-events: none;
}

/* ── Transitions ── */
.fade-slide-enter-active { transition: all 200ms var(--ease-out); }
.fade-slide-leave-active { transition: all 150ms var(--ease-out); }
.fade-slide-enter-from { opacity: 0; transform: translateX(-8px); }
.fade-slide-leave-to { opacity: 0; }

.sidebar-brand.collapsed { justify-content: center; padding: 0; }
</style>
