<template>
  <div class="dashboard">
    <!-- Stat cards -->
    <a-row :gutter="24" class="stat-row">
      <a-col :xs="24" :sm="12" :lg="6" v-for="s in stats" :key="s.key">
        <a-card class="stat-card" :bordered="false">
          <div class="stat-inner">
            <div class="stat-icon" :style="{ background: s.color }">
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

    <!-- Quick actions -->
    <a-row :gutter="24" style="margin-top: var(--space-6)">
      <a-col :span="16">
        <a-card title="今日概览" :bordered="false">
          <a-empty description="暂无数据" v-if="true" />
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="快捷操作" :bordered="false">
          <div class="quick-actions">
            <a-button type="link" block @click="$router.push('/product')">
              <shopping-outlined /> 添加商品
            </a-button>
            <a-button type="link" block @click="$router.push('/order')">
              <unordered-list-outlined /> 查看订单
            </a-button>
            <a-button type="link" block @click="$router.push('/member')">
              <user-outlined /> 会员管理
            </a-button>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import {
  ShoppingOutlined, UnorderedListOutlined,
  UserOutlined, DollarOutlined
} from '@ant-design/icons-vue'

const stats = [
  { key: 'product', label: '商品总数',   value: '--', icon: 'shopping-outlined',      color: 'rgba(59,130,246,0.1)' },
  { key: 'order',   label: '今日订单',   value: '--', icon: 'unordered-list-outlined', color: 'rgba(200,150,62,0.12)' },
  { key: 'member',  label: '会员总数',   value: '--', icon: 'user-outlined',           color: 'rgba(45,138,86,0.1)' },
  { key: 'refund',  label: '待处理退款', value: '--', icon: 'dollar-outlined',         color: 'rgba(197,48,48,0.08)' }
]
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
}

.stat-card {
  overflow: hidden;
}

.stat-inner {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  font-size: 22px;
  color: var(--color-lead);
}

.stat-body {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: var(--text-sm);
  color: var(--color-slate);
}

.stat-value {
  font-size: var(--text-2xl);
  font-weight: 600;
  color: var(--color-lead);
  font-variant-numeric: tabular-nums;
}

.quick-actions {
  display: flex;
  flex-direction: column;
}
.quick-actions .ant-btn-link {
  text-align: left;
  padding: var(--space-2) var(--space-3);
  color: var(--color-lead);
}
.quick-actions .ant-btn-link:hover {
  color: var(--color-brass);
}
</style>
