<template>
  <a-layout class="admin-layout">
    <!-- Sidebar -->
    <a-layout-sider
      v-model:collapsed="collapsed"
      :width="220"
      :collapsed-width="64"
      class="admin-sidebar"
      :trigger="null"
      collapsible
    >
      <!-- Brand -->
      <div class="sidebar-brand" :class="{ collapsed }">
        <div class="brand-icon">CB</div>
        <transition name="fade-slide">
          <span v-if="!collapsed" class="brand-text">跨境管理</span>
        </transition>
      </div>

      <!-- Menu sections -->
      <div class="menu-section">
        <transition name="fade-slide">
          <span v-if="!collapsed" class="menu-label">业务管理</span>
        </transition>
        <a-menu v-model:selectedKeys="selectedKeys" mode="inline" class="sidebar-menu" @click="handleMenuClick">
          <a-menu-item key="/dashboard">
            <template #icon><dashboard-outlined /></template>
            <span>工作台</span>
          </a-menu-item>
          <a-menu-item key="/product">
            <template #icon><shopping-outlined /></template>
            <span>商品管理</span>
          </a-menu-item>
          <a-menu-item key="/order">
            <template #icon><unordered-list-outlined /></template>
            <span>订单管理</span>
          </a-menu-item>
          <a-menu-item key="/member">
            <template #icon><user-outlined /></template>
            <span>会员管理</span>
          </a-menu-item>
        </a-menu>
      </div>

      <div class="menu-section">
        <transition name="fade-slide">
          <span v-if="!collapsed" class="menu-label">运营设置</span>
        </transition>
        <a-menu v-model:selectedKeys="selectedKeys2" mode="inline" class="sidebar-menu" @click="handleMenuClick">
          <a-menu-item key="/marketing">
            <template #icon><gift-outlined /></template>
            <span>营销管理</span>
          </a-menu-item>
          <a-menu-item key="/finance">
            <template #icon><dollar-outlined /></template>
            <span>财务管理</span>
          </a-menu-item>
          <a-menu-item key="/system">
            <template #icon><setting-outlined /></template>
            <span>系统设置</span>
          </a-menu-item>
        </a-menu>
      </div>

      <!-- Collapse toggle -->
      <div class="sidebar-footer">
        <div class="sidebar-toggle" @click="collapsed = !collapsed">
          <menu-fold-outlined v-if="!collapsed" />
          <menu-unfold-outlined v-else />
        </div>
      </div>
    </a-layout-sider>

    <!-- Main area -->
    <a-layout class="admin-main">
      <a-layout-header class="admin-header">
        <div class="header-left">
          <span class="header-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <a-dropdown placement="bottomRight">
            <div class="user-chip">
              <a-avatar :size="30" class="user-avatar">
                <template #icon><user-outlined /></template>
              </a-avatar>
              <span class="user-name">{{ userStore.userInfo?.realName || '管理员' }}</span>
              <down-outlined class="user-arrow" />
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="logout" @click="handleLogout">
                  <logout-outlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content class="admin-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import {
  DashboardOutlined, ShoppingOutlined, UnorderedListOutlined,
  UserOutlined, GiftOutlined, DollarOutlined, SettingOutlined,
  MenuFoldOutlined, MenuUnfoldOutlined, DownOutlined, LogoutOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const collapsed = ref(false)
const selectedKeys = ref(['/dashboard'])
const selectedKeys2 = ref([])

const pageTitle = computed(() => route.meta?.title || '工作台')

function handleMenuClick({ key }) {
  router.push(key)
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

/* ---- Sidebar ---- */
.admin-sidebar {
  background: var(--color-abyss) !important;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-brand {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: 0 var(--space-4);
  border-bottom: 1px solid rgba(255,255,255,0.06);
  background: linear-gradient(180deg, rgba(255,255,255,0.02) 0%, transparent 100%);
}

.brand-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-brass);
  color: var(--color-abyss);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: -0.5px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.brand-text {
  color: #fff;
  font-size: var(--text-lg);
  font-weight: 600;
  letter-spacing: 2px;
  white-space: nowrap;
}

/* Menu section labels */
.menu-section {
  padding: var(--space-2) 0;
}

.menu-section + .menu-section {
  border-top: 1px solid rgba(255,255,255,0.04);
  margin-top: var(--space-2);
  padding-top: var(--space-3);
}

.menu-label {
  display: block;
  padding: 0 var(--space-5);
  margin-bottom: var(--space-1);
  font-size: 10px;
  font-weight: 600;
  color: rgba(255,255,255,0.32);
  text-transform: uppercase;
  letter-spacing: 1.5px;
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
  color: rgba(255,255,255,0.72) !important;
  font-weight: 400;
  transition: all var(--duration-fast) var(--ease-out);
  width: auto !important;
}
.sidebar-menu .ant-menu-item:hover {
  color: rgba(255,255,255,0.95) !important;
  background: rgba(255,255,255,0.08) !important;
}
.sidebar-menu .ant-menu-item.ant-menu-item-selected {
  background: var(--color-brass-soft) !important;
  color: var(--color-brass) !important;
  font-weight: 500;
}
.sidebar-menu .ant-menu-item.ant-menu-item-selected::before {
  content: '';
  position: absolute;
  left: -4px;
  top: 10px;
  bottom: 10px;
  width: 3px;
  background: var(--color-brass);
  border-radius: 0 3px 3px 0;
}
.sidebar-menu .ant-menu-item .anticon {
  font-size: 17px;
}

/* Footer */
.sidebar-footer {
  margin-top: auto;
  border-top: 1px solid rgba(255,255,255,0.06);
}

.sidebar-toggle {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255,255,255,0.3);
  cursor: pointer;
  font-size: 15px;
  transition: all var(--duration-fast) var(--ease-out);
}
.sidebar-toggle:hover {
  color: rgba(255,255,255,0.65);
  background: rgba(255,255,255,0.04);
}

/* ---- Main ---- */
.admin-main {
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.admin-header {
  height: var(--header-height) !important;
  line-height: var(--header-height) !important;
  background: var(--color-surface) !important;
  padding: 0 var(--space-6) !important;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--color-fog);
  flex-shrink: 0;
}

.header-title {
  font-size: var(--text-xl);
  font-weight: 600;
  color: var(--color-lead);
  letter-spacing: -0.2px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-md);
  transition: background var(--duration-fast) var(--ease-out);
}
.user-chip:hover {
  background: var(--color-fog);
}

.user-avatar {
  background: var(--color-fog) !important;
  flex-shrink: 0;
}
.user-avatar .anticon {
  color: var(--color-slate);
}

.user-name {
  font-size: var(--text-sm);
  color: var(--color-lead);
  font-weight: 500;
}

.user-arrow {
  font-size: 10px;
  color: var(--color-slate-light);
  margin-left: 2px;
}

.admin-content {
  flex: 1;
  padding: var(--space-6);
  overflow-y: auto;
  background:
    linear-gradient(180deg, var(--color-surface) 0%, var(--color-fog) 40px);
}

/* Transitions */
.fade-slide-enter-active {
  transition: all 200ms var(--ease-out);
}
.fade-slide-leave-active {
  transition: all 150ms var(--ease-out);
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(-8px);
}
.fade-slide-leave-to {
  opacity: 0;
}

.sidebar-brand.collapsed {
  justify-content: center;
  padding: 0;
}
</style>
