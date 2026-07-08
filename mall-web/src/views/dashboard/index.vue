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
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" :style="{ color: s.fg }" v-html="s.icon"></svg>
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
            <p class="chart-empty-hint">订单趋势数据将在此展示</p>
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
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { getDashboardStats } from '@/api/dashboard'

const userStore = useUserStore()

const today = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})

const stats = ref([
  { key: 'product', label: '商品总数',    value: 0, bg: 'rgba(59,130,246,0.08)', fg: '#3B82F6', trendDir: 'up',   trendText: '--',
    icon: '<path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.29 7 12 12 20.71 7"/><line x1="12" y1="22" x2="12" y2="12"/>' },
  { key: 'order',   label: '今日订单',    value: 0, bg: 'rgba(200,150,62,0.08)',  fg: '#C8963E', trendDir: 'up',   trendText: '--',
    icon: '<path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/><line x1="9" y1="12" x2="15" y2="12"/><line x1="9" y1="16" x2="13" y2="16"/>' },
  { key: 'member',  label: '会员总数',    value: 0, bg: 'rgba(45,138,86,0.08)',   fg: '#2D8A56', trendDir: 'up',   trendText: '--',
    icon: '<path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><polyline points="17 11 19 13 23 9"/>' },
  { key: 'refund',  label: '待处理退款',  value: 0, bg: 'rgba(197,48,48,0.08)',   fg: '#C53030', trendDir: 'down', trendText: '--',
    icon: '<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>' }
])

const barHeights = Array.from({ length: 24 }, (_, i) => {
  // Seeded pseudo-random — stable across re-renders
  const seed = Math.sin(i * 1.7 + 3.1) * 10000
  return Math.floor((seed - Math.floor(seed)) * 35) + 18
})

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    const d = res.data
    stats.value[0].value = d.productCount ?? 0
    stats.value[1].value = d.todayOrders ?? 0
    stats.value[2].value = d.memberCount ?? 0
    stats.value[3].value = d.pendingRefund ?? 0
  } catch {
    // keep placeholder values on error
  }
})
</script>

<style scoped>
/* ════════════════════════════════════════
   DASHBOARD — Harbor Control Instruments
   ════════════════════════════════════════ */

.dashboard {
  max-width: 1200px;
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
  animation: dash-enter 0.5s var(--ease-out) both;
}

@keyframes dash-enter {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ── Page header ── */
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.page-title {
  font-family: var(--font-display);
  font-size: var(--text-2xl);
  font-weight: 600;
  color: var(--color-lead);
  letter-spacing: -0.2px;
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

/* Instrument-style card */
.kpi-card {
  background: linear-gradient(175deg,
    var(--color-surface) 0%,
    var(--color-ivory) 100%
  );
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  box-shadow:
    var(--shadow-card),
    inset 0 1px 0 rgba(255,255,255,0.8);
  transition: all var(--duration-normal) var(--ease-out);
  cursor: default;
  position: relative;
  overflow: hidden;
}

/* Brass top accent — like an instrument bezel */
.kpi-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: var(--space-2);
  right: var(--space-2);
  height: 1px;
  background: linear-gradient(90deg,
    transparent 0%,
    color-mix(in srgb, var(--accent) 40%, var(--color-brass)) 20%,
    var(--accent) 50%,
    color-mix(in srgb, var(--accent) 40%, var(--color-brass)) 80%,
    transparent 100%
  );
  opacity: 0.5;
}

/* Corner tick marks — navigation instrument detail */
.kpi-card::after {
  content: '';
  position: absolute;
  top: 6px;
  right: 10px;
  width: 6px;
  height: 6px;
  border-top: 1px solid color-mix(in srgb, var(--accent) 30%, transparent);
  border-right: 1px solid color-mix(in srgb, var(--accent) 30%, transparent);
  opacity: 0.4;
}

.kpi-card:hover {
  transform: translateY(-4px);
  box-shadow:
    var(--shadow-elevated),
    0 0 0 1px var(--accent-soft),
    0 0 24px var(--accent-soft);
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-3);
}

.kpi-label {
  font-size: 10px;
  color: var(--color-slate);
  text-transform: uppercase;
  letter-spacing: 1.2px;
  font-weight: 600;
}

.kpi-icon-row {
  display: flex;
  align-items: center;
  padding: 4px;
  border-radius: 50%;
  background: var(--accent-soft);
}

.kpi-value {
  font-family: var(--font-display);
  font-size: 38px;
  font-weight: 600;
  color: var(--color-lead);
  letter-spacing: -1px;
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
  font-size: 10px;
  color: var(--color-slate-light);
  letter-spacing: 0.8px;
  text-transform: uppercase;
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

/* ── Chart — Depth Sounder ── */
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
  gap: 3px;
  height: 160px;
  position: relative;
}

/* Background grid lines */
.chart-bars::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    repeating-linear-gradient(0deg, transparent, transparent 31px, rgba(200,150,62,0.04) 31px, rgba(200,150,62,0.04) 32px);
  pointer-events: none;
}

/* Scanning sonar line */
.chart-bars::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg,
    transparent, rgba(200,150,62,0.15) 20%, rgba(200,150,62,0.25) 50%, rgba(200,150,62,0.15) 80%, transparent
  );
  pointer-events: none;
  animation: sonar-scan 4s ease-in-out infinite;
}
@keyframes sonar-scan {
  0%, 100% { top: 20%; opacity: 0.3; }
  25%  { top: 60%; opacity: 0.8; }
  50%  { top: 40%; opacity: 0.5; }
  75%  { top: 75%; opacity: 0.7; }
}

.chart-bar {
  flex: 1;
  border-radius: 3px 3px 0 0;
  background: linear-gradient(180deg,
    var(--color-brass) 0%,
    rgba(200, 150, 62, 0.35) 100%
  );
  opacity: 0.45;
  min-height: 4px;
  animation: bar-rise 0.7s var(--ease-out) both;
  transition: opacity var(--duration-normal);
  position: relative;
}

/* Glow tip on each bar */
.chart-bar::after {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 80%;
  height: 2px;
  background: var(--color-brass);
  border-radius: 2px;
  opacity: 0.6;
}

.chart-bar:hover {
  opacity: 0.75;
}

.chart-bar:hover::after {
  opacity: 1;
  box-shadow: 0 0 6px var(--color-brass-glow);
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

/* ── Quick actions — Signal Flags ── */
.quick-grid {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
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
  position: relative;
}

/* Left accent line on hover — like a flag edge */
.quick-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 2px;
  background: var(--color-brass);
  border-radius: 0 2px 2px 0;
  transform: scaleY(0);
  transition: transform var(--duration-fast) var(--ease-out);
}

.quick-item:hover {
  background: rgba(200, 150, 62, 0.04);
  color: var(--color-lead);
}
.quick-item:hover::before {
  transform: scaleY(1);
}

.quick-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  background: var(--qi-bg);
  color: var(--qi-fg);
  flex-shrink: 0;
  transition: all var(--duration-fast) var(--ease-out);
}

.quick-item:hover .quick-icon {
  border-radius: var(--radius-md);
  transform: scale(1.05);
}

.quick-label {
  flex: 1;
  font-weight: 500;
}

.quick-arrow {
  color: var(--color-slate-light);
  font-size: 16px;
  transition: all var(--duration-fast) var(--ease-out);
}
.quick-item:hover .quick-arrow {
  transform: translateX(4px);
  color: var(--color-brass);
}

/* ── Animations ── */
.kpi-card:nth-child(1) { animation: card-in 0.45s var(--ease-out) 0.05s both; }
.kpi-card:nth-child(2) { animation: card-in 0.45s var(--ease-out) 0.12s both; }
.kpi-card:nth-child(3) { animation: card-in 0.45s var(--ease-out) 0.19s both; }
.kpi-card:nth-child(4) { animation: card-in 0.45s var(--ease-out) 0.26s both; }

@keyframes card-in {
  from { opacity: 0; transform: translateY(16px); }
  to   { opacity: 1; transform: translateY(0); }
}
</style>
