<template>
  <div class="dashboard">
    <!-- Page header -->
    <div class="page-head">
      <div>
        <h2 class="page-title">工作台</h2>
        <p class="page-desc">欢迎回来，{{ userStore.userInfo?.realName || '管理员' }} · {{ today }}</p>
      </div>
      <div class="head-actions">
        <a-button class="action-btn" @click="$router.push('/order')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M16 17l5-5-5-5"/><path d="M21 12H9"/></svg>
          查看订单
        </a-button>
        <a-button type="primary" @click="$router.push('/product')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          添加商品
        </a-button>
      </div>
    </div>

    <!-- KPI Card Grid -->
    <div class="kpi-grid">
      <div v-for="s in stats" :key="s.key" class="kpi-card" :style="{ '--accent': s.fg, '--accent-soft': s.bg }">
        <div class="kpi-top">
          <span class="kpi-label">{{ s.label }}</span>
          <span class="kpi-icon-row">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" :style="{ color: s.fg }"><component :is="s.iconPath" /></svg>
          </span>
        </div>
        <div class="kpi-value">{{ s.value }}</div>
        <div class="kpi-footer">
          <span class="kpi-trend" :class="s.trendDir">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline v-if="s.trendDir === 'up'" points="18 15 12 9 6 15"/>
              <polyline v-else points="6 9 12 15 18 9"/>
            </svg>
            <span>{{ s.trendText }}</span>
          </span>
          <span class="kpi-period">今日</span>
        </div>
      </div>
    </div>

    <!-- Main content grid -->
    <div class="dashboard-grid">
      <!-- Chart area -->
      <div class="grid-panel panel-chart">
        <div class="panel-header">
          <h3 class="panel-title">今日概览</h3>
          <div class="panel-badges">
            <span class="badge badge-live">实时</span>
            <span class="badge badge-sub">过去 24 小时</span>
          </div>
        </div>
        <div class="panel-body">
          <div class="chart-placeholder">
            <div class="chart-bars">
              <div v-for="i in 24" :key="i" class="chart-bar" :style="{ height: barHeights[i-1] + '%', animationDelay: (i * 0.04) + 's' }"></div>
            </div>
            <div class="chart-label-row">
              <span>00:00</span>
              <span>06:00</span>
              <span>12:00</span>
              <span>18:00</span>
              <span>24:00</span>
            </div>
            <p class="chart-empty-hint">连接后端后显示实时订单趋势</p>
          </div>
        </div>
      </div>

      <!-- Quick actions -->
      <div class="grid-panel panel-quick">
        <div class="panel-header">
          <h3 class="panel-title">快捷操作</h3>
        </div>
        <div class="panel-body">
          <div class="quick-grid">
            <button class="quick-item" @click="$router.push('/product')">
              <span class="quick-icon" style="--qi-bg:#EFF6FF;--qi-fg:#3B82F6">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>
              </span>
              <span class="quick-label">添加商品</span>
              <span class="quick-arrow">&rarr;</span>
            </button>
            <button class="quick-item" @click="$router.push('/order')">
              <span class="quick-icon" style="--qi-bg:#FDF6EE;--qi-fg:#C8963E">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/></svg>
              </span>
              <span class="quick-label">查看订单</span>
              <span class="quick-arrow">&rarr;</span>
            </button>
            <button class="quick-item" @click="$router.push('/member')">
              <span class="quick-icon" style="--qi-bg:#EDF7F1;--qi-fg:#2D8A56">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/></svg>
              </span>
              <span class="quick-label">会员管理</span>
              <span class="quick-arrow">&rarr;</span>
            </button>
            <button class="quick-item" @click="$router.push('/marketing')">
              <span class="quick-icon" style="--qi-bg:#F5F3FF;--qi-fg:#7C3AED">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
              </span>
              <span class="quick-label">营销活动</span>
              <span class="quick-arrow">&rarr;</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const today = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})

const stats = [
  { key: 'product', label: '商品总数',    value: '--', iconPath: 'path-d', bg: 'rgba(59,130,246,0.08)', fg: '#3B82F6', trendDir: 'up',   trendText: '+12.5%' },
  { key: 'order',   label: '今日订单',    value: '--', iconPath: 'path-l', bg: 'rgba(200,150,62,0.08)',  fg: '#C8963E', trendDir: 'up',   trendText: '+5.2%' },
  { key: 'member',  label: '会员总数',    value: '--', iconPath: 'path-u', bg: 'rgba(45,138,86,0.08)',   fg: '#2D8A56', trendDir: 'up',   trendText: '+3.8%' },
  { key: 'refund',  label: '待处理退款',  value: '--', iconPath: 'path-d', bg: 'rgba(197,48,48,0.08)',   fg: '#C53030', trendDir: 'down', trendText: '-2.1%' }
]

const barHeights = Array.from({ length: 24 }, () => Math.floor(Math.random() * 60) + 15)
</script>

<style scoped>
/* ════════════════════════════════════════
   DASHBOARD — Command Center
   ════════════════════════════════════════ */

.dashboard {
  max-width: 1200px;
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

/* ── Page header ── */
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.page-title {
  font-family: var(--font-sans);
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--color-lead);
  letter-spacing: -0.3px;
  line-height: 1.2;
  margin-bottom: 2px;
}

.page-desc {
  font-size: var(--text-sm);
  color: var(--color-slate-light);
  letter-spacing: 0.2px;
}

.head-actions {
  display: flex;
  gap: var(--space-2);
}

.action-btn {
  display: inline-flex !important;
  align-items: center;
  gap: 6px;
  font-size: var(--text-sm) !important;
  border-radius: var(--radius-md) !important;
}

/* ── KPI card grid ── */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
}

@media (max-width: 900px) {
  .kpi-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 500px) {
  .kpi-grid { grid-template-columns: 1fr; }
}

.kpi-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  box-shadow: var(--shadow-card);
  transition: all var(--duration-normal) var(--ease-out);
  cursor: default;
  position: relative;
  overflow: hidden;
}

.kpi-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--accent), color-mix(in srgb, var(--accent) 60%, var(--color-brass)));
  opacity: 0.6;
}

.kpi-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-elevated), 0 0 0 1px var(--accent-soft);
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-3);
}

.kpi-label {
  font-size: var(--text-xs);
  color: var(--color-slate);
  text-transform: uppercase;
  letter-spacing: 0.8px;
  font-weight: 500;
}

.kpi-icon-row {
  display: flex;
  align-items: center;
  opacity: 0.5;
}

.kpi-value {
  font-family: var(--font-sans);
  font-size: var(--text-3xl);
  font-weight: 700;
  color: var(--color-lead);
  letter-spacing: -0.5px;
  line-height: 1;
  margin-bottom: var(--space-3);
  font-variant-numeric: tabular-nums;
}

.kpi-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi-trend {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  font-weight: 500;
}
.kpi-trend.up { color: var(--color-success); }
.kpi-trend.down { color: var(--color-danger); }

.kpi-period {
  font-size: 11px;
  color: var(--color-slate-light);
  letter-spacing: 0.5px;
}

/* ── Dashboard grid ── */
.dashboard-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: var(--space-4);
}

@media (max-width: 800px) {
  .dashboard-grid { grid-template-columns: 1fr; }
}

.grid-panel {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid rgba(11, 25, 44, 0.04);
}

.panel-title {
  font-family: var(--font-sans);
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--color-lead);
}

.panel-badges {
  display: flex;
  gap: var(--space-2);
}

.badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
  letter-spacing: 0.3px;
}
.badge-live {
  background: rgba(45, 138, 86, 0.1);
  color: var(--color-success);
}
.badge-sub {
  background: rgba(11, 25, 44, 0.04);
  color: var(--color-slate-light);
}

.panel-body {
  padding: var(--space-5);
}

/* ── Chart placeholder ── */
.chart-placeholder {
  position: relative;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 160px;
}

.chart-bar {
  flex: 1;
  border-radius: 2px 2px 0 0;
  background: linear-gradient(180deg, var(--color-brass) 0%, rgba(200, 150, 62, 0.3) 100%);
  opacity: 0.4;
  min-height: 4px;
  animation: bar-rise 0.6s var(--ease-out) both;
  transition: opacity var(--duration-normal);
}

.chart-bar:hover {
  opacity: 0.7;
}

@keyframes bar-rise {
  from { height: 4px !important; }
}

.chart-label-row {
  display: flex;
  justify-content: space-between;
  margin-top: var(--space-2);
  font-size: 10px;
  color: var(--color-slate-light);
  letter-spacing: 0.5px;
}

.chart-empty-hint {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-sm);
  color: var(--color-slate-light);
  letter-spacing: 0.3px;
  pointer-events: none;
}

/* ── Quick actions ── */
.quick-grid {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-3);
  border: none;
  background: transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  width: 100%;
  text-align: left;
  font-family: var(--font-sans);
  font-size: var(--text-sm);
  color: var(--color-lead);
}
.quick-item:hover {
  background: rgba(11, 25, 44, 0.03);
  padding-left: var(--space-4);
  color: var(--color-brass);
}

.quick-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: var(--qi-bg);
  color: var(--qi-fg);
  flex-shrink: 0;
}

.quick-label {
  flex: 1;
  font-weight: 500;
}

.quick-arrow {
  color: var(--color-slate-light);
  font-size: 16px;
  transition: transform var(--duration-fast) var(--ease-out);
}
.quick-item:hover .quick-arrow {
  transform: translateX(3px);
  color: var(--color-brass);
}
</style>
