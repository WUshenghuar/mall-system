<template>
  <div class="dashboard">
    <!-- Stat cards -->
    <a-row :gutter="24" class="stat-row">
      <a-col :xs="24" :sm="12" :lg="6" v-for="s in stats" :key="s.key">
        <a-card class="stat-card" :bordered="false" hoverable>
          <div class="stat-inner">
            <div class="stat-icon-box" :style="{ background: s.bg, color: s.fg }">
              <component :is="s.icon" />
            </div>
            <div class="stat-body">
              <span class="stat-label">{{ s.label }}</span>
              <span class="stat-value">{{ s.value }}</span>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- Charts + quick actions -->
    <a-row :gutter="24" class="content-row">
      <a-col :xs="24" :lg="16">
        <a-card title="今日概览" :bordered="false" class="overview-card">
          <template #extra>
            <a-tag color="processing">实时</a-tag>
          </template>
          <a-empty description="连接后端后显示今日订单趋势" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card title="快捷操作" :bordered="false">
          <div class="quick-actions">
            <a-button type="text" block @click="$router.push('/product')">
              <shopping-outlined /> 添加商品
            </a-button>
            <a-button type="text" block @click="$router.push('/order')">
              <unordered-list-outlined /> 查看订单
            </a-button>
            <a-button type="text" block @click="$router.push('/member')">
              <user-outlined /> 会员管理
            </a-button>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { h } from 'vue'
import {
  ShoppingOutlined, UnorderedListOutlined,
  UserOutlined, DollarOutlined
} from '@ant-design/icons-vue'

const stats = [
  { key: 'product', label: '商品总数',   value: '--', icon: ShoppingOutlined,     bg: '#EFF6FF', fg: '#3B82F6' },
  { key: 'order',   label: '今日订单',   value: '--', icon: UnorderedListOutlined, bg: '#FDF6EE', fg: '#C8963E' },
  { key: 'member',  label: '会员总数',   value: '--', icon: UserOutlined,         bg: '#EDF7F1', fg: '#2D8A56' },
  { key: 'refund',  label: '待处理退款', value: '--', icon: DollarOutlined,       bg: '#FDF2F2', fg: '#C53030' }
]
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
}

.stat-row {
  margin-bottom: var(--space-6);
}

.stat-card {
  border-radius: var(--radius-lg);
  transition: transform var(--duration-fast) var(--ease-out),
              box-shadow var(--duration-fast) var(--ease-out);
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-elevated);
}

.stat-inner {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
}

.stat-icon-box {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  font-size: 20px;
  flex-shrink: 0;
}

.stat-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stat-label {
  font-size: var(--text-xs);
  color: var(--color-slate);
  letter-spacing: 0.3px;
  text-transform: uppercase;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-lead);
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
  letter-spacing: -0.5px;
}

.content-row {
  margin-top: var(--space-6);
}

.overview-card .ant-card-body {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.quick-actions .ant-btn-text {
  text-align: left;
  padding: var(--space-3) var(--space-4);
  color: var(--color-lead);
  border-radius: var(--radius-md);
  height: auto;
  justify-content: flex-start;
}
.quick-actions .ant-btn-text:hover {
  background: var(--color-fog);
  color: var(--color-brass);
}
</style>
