<template>
  <div class="login-page">
    <!-- Animated background layers -->
    <div class="bg-layer">
      <div class="grid-map"></div>
      <div class="grid-meridians"></div>
      <div class="glow-center"></div>
      <div class="glow-edge"></div>
      <div class="grain-overlay"></div>
    </div>

    <!-- Animated trade routes -->
    <svg class="trade-routes" viewBox="0 0 1200 800" preserveAspectRatio="xMidYMid slice">
      <!-- Route 1: East-West -->
      <path d="M100,400 Q300,300 600,350 T1100,300" class="route-line route-1" />
      <circle r="3" class="route-dot dot-1" />
      <!-- Route 2: North-South -->
      <path d="M700,100 Q600,300 650,500 T800,700" class="route-line route-2" />
      <circle r="3" class="route-dot dot-2" />
      <!-- Route 3: Diagonal -->
      <path d="M200,600 Q400,400 700,450 T1000,200" class="route-line route-3" />
      <circle r="3" class="route-dot dot-3" />
      <!-- Route 4: Southern arc -->
      <path d="M50,650 Q350,750 700,600 T1150,550" class="route-line route-4" />
      <circle r="3" class="route-dot dot-4" />
    </svg>

    <!-- Navigation dots (port cities) -->
    <div class="port-cities">
      <span class="port port-1" style="top:35%;left:12%">上海</span>
      <span class="port port-2" style="top:30%;left:45%">Singapore</span>
      <span class="port port-3" style="top:25%;left:78%">Hamburg</span>
      <span class="port port-4" style="top:55%;left:30%">Dubai</span>
      <span class="port port-5" style="top:45%;left:65%">Rotterdam</span>
    </div>

    <!-- Login card -->
    <div class="login-stage">
      <div class="login-card">
        <!-- Left decorative panel -->
        <div class="card-herald">
          <div class="herald-content">
            <div class="herald-emblem">
              <svg viewBox="0 0 80 80" class="emblem-svg">
                <!-- Compass rose -->
                <circle cx="40" cy="40" r="36" fill="none" stroke="currentColor" stroke-width="0.8" opacity="0.3"/>
                <circle cx="40" cy="40" r="22" fill="none" stroke="currentColor" stroke-width="0.6" opacity="0.2"/>
                <!-- N S E W -->
                <line x1="40" y1="8" x2="40" y2="14" stroke="currentColor" stroke-width="1.5"/>
                <line x1="40" y1="66" x2="40" y2="72" stroke="currentColor" stroke-width="1"/>
                <line x1="4" y1="40" x2="10" y2="40" stroke="currentColor" stroke-width="1"/>
                <line x1="70" y1="40" x2="76" y2="40" stroke="currentColor" stroke-width="1"/>
                <!-- Diamond -->
                <polygon points="40,16 58,40 40,64 22,40" fill="none" stroke="currentColor" stroke-width="1.2" opacity="0.6"/>
                <polygon points="40,24 50,40 40,56 30,40" fill="currentColor" opacity="0.15"/>
                <!-- Center dot -->
                <circle cx="40" cy="40" r="2" fill="currentColor"/>
              </svg>
            </div>
            <div class="herald-brand">
              <span class="herald-cb">CB</span>
              <span class="herald-dot"></span>
            </div>
            <h2 class="herald-title">跨境管理平台</h2>
            <p class="herald-sub">Cross-Border<br/>Trade Command</p>
            <div class="herald-line"></div>
            <p class="herald-tagline">全球通达 · 货通天下</p>
          </div>
        </div>

        <!-- Right form panel -->
        <div class="card-form">
          <div class="form-header">
            <h3 class="form-title">欢迎回来</h3>
            <p class="form-desc">登录您的管理账户</p>
          </div>

          <a-form
            class="login-form"
            :model="form"
            @finish="handleLogin"
            autocomplete="off"
          >
            <a-form-item
              name="username"
              :rules="[{ required: true, message: '请输入用户名' }]"
            >
              <a-input
                v-model:value="form.username"
                placeholder="用户名 / Username"
                size="large"
              >
                <template #prefix>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="8" r="4"/><path d="M4 21v-1a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v1"/></svg>
                </template>
              </a-input>
            </a-form-item>

            <a-form-item
              name="password"
              :rules="[{ required: true, message: '请输入密码' }]"
            >
              <a-input-password
                v-model:value="form.password"
                placeholder="密码 / Password"
                size="large"
              >
                <template #prefix>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                </template>
              </a-input-password>
            </a-form-item>

            <a-form-item class="form-submit-item">
              <a-button
                type="primary"
                html-type="submit"
                size="large"
                block
                :loading="loading"
                class="login-btn"
              >
                <span>进入管理后台</span>
                <svg v-if="!loading" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="btn-arrow"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
              </a-button>
            </a-form-item>
          </a-form>

          <div class="form-footer">
            <span class="footer-line"></span>
            <p class="footer-text">B2C Cross-Border E-Commerce v1.0</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const form = reactive({ username: 'admin', password: 'admin123' })
const loading = ref(false)

async function handleLogin() {
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    router.push('/dashboard')
  } catch {
    // handled by request interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ════════════════════════════════════════
   LOGIN PAGE — Global Trade Command
   ════════════════════════════════════════ */

.login-page {
  position: relative;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background: var(--color-abyss);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-sans);
}

/* ── Background layers ── */
.bg-layer {
  position: absolute;
  inset: 0;
  z-index: 0;
}

/* World map grid (latitude/longitude lines) */
.grid-map {
  position: absolute;
  inset: 0;
  background-image:
    /* Latitude lines */
    repeating-linear-gradient(0deg, transparent, transparent 39px, rgba(200, 150, 62, 0.025) 39px, rgba(200, 150, 62, 0.025) 40px),
    /* Longitude lines */
    repeating-linear-gradient(90deg, transparent, transparent 39px, rgba(200, 150, 62, 0.025) 39px, rgba(200, 150, 62, 0.025) 40px);
  mask-image: radial-gradient(ellipse 1000px 600px at 50% 50%, black, transparent 70%);
  -webkit-mask-image: radial-gradient(ellipse 1000px 600px at 50% 50%, black, transparent 70%);
}

/* Meridians - curved lines */
.grid-meridians {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(ellipse 1200px 800px at 20% 50%, transparent 20%, rgba(200, 150, 62, 0.015) 20%, transparent 20.5%),
    radial-gradient(ellipse 1000px 700px at 80% 40%, transparent 25%, rgba(200, 150, 62, 0.012) 25%, transparent 25.5%);
}

/* Warm glow centers */
.glow-center {
  position: absolute;
  width: 700px;
  height: 700px;
  top: 50%;
  left: 30%;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(200, 150, 62, 0.06) 0%, transparent 60%);
  pointer-events: none;
}

.glow-edge {
  position: absolute;
  width: 500px;
  height: 500px;
  top: 20%;
  right: -10%;
  background: radial-gradient(circle, rgba(200, 150, 62, 0.04) 0%, transparent 50%);
  pointer-events: none;
}

/* Grain texture overlay */
.grain-overlay {
  position: absolute;
  inset: 0;
  opacity: 0.15;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-repeat: repeat;
  background-size: 256px 256px;
  pointer-events: none;
  mix-blend-mode: overlay;
}

/* ── Animated trade route SVG ── */
.trade-routes {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
  opacity: 0.25;
}

.route-line {
  fill: none;
  stroke: var(--color-brass);
  stroke-width: 1.2;
  stroke-dasharray: 8 6;
  opacity: 0.5;
}

.route-1 { animation: route-drift 18s ease-in-out infinite; }
.route-2 { animation: route-drift 22s ease-in-out infinite reverse; }
.route-3 { animation: route-drift 26s ease-in-out infinite; }
.route-4 { animation: route-drift 20s ease-in-out infinite reverse; }

@keyframes route-drift {
  0%, 100% { stroke-dashoffset: 0; opacity: 0.4; }
  50% { stroke-dashoffset: 28; opacity: 0.7; }
}

.route-dot {
  fill: var(--color-brass);
  filter: drop-shadow(0 0 4px rgba(200, 150, 62, 0.5));
  animation: dot-float 8s ease-in-out infinite;
}

.dot-1 { animation-delay: 0s; }
.dot-2 { animation-delay: -2s; }
.dot-3 { animation-delay: -4s; }
.dot-4 { animation-delay: -6s; }

@keyframes dot-float {
  0%, 100% {
    cx: 150; cy: 380;
    opacity: 0;
  }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% {
    cx: 1050; cy: 320;
    opacity: 0;
  }
}

/* ── Port city markers ── */
.port-cities {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
}

.port {
  position: absolute;
  font-size: 10px;
  color: rgba(200, 150, 62, 0.35);
  letter-spacing: 1.5px;
  text-transform: uppercase;
  font-weight: 500;
  transform: translate(-50%, -50%);
}

.port::before {
  content: '';
  display: block;
  width: 4px;
  height: 4px;
  background: var(--color-brass);
  border-radius: 50%;
  margin: 0 auto 4px;
  box-shadow: 0 0 6px rgba(200, 150, 62, 0.4);
}

/* ── Login stage (centered) ── */
.login-stage {
  position: relative;
  z-index: 10;
  width: 880px;
  max-width: 94vw;
  animation: stage-enter 0.8s var(--ease-out) both;
}

@keyframes stage-enter {
  from { opacity: 0; transform: translateY(20px) scale(0.98); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

/* ── Main login card ── */
.login-card {
  display: flex;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(20px) saturate(1.2);
  -webkit-backdrop-filter: blur(20px) saturate(1.2);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.3),
    0 0 0 1px rgba(255, 255, 255, 0.06) inset;
}

/* ── Left: Heraldic panel ── */
.card-herald {
  flex: 0 0 300px;
  background: linear-gradient(160deg, var(--color-abyss) 0%, #132236 50%, var(--color-night) 100%);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-8);
}

.card-herald::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(200, 150, 62, 0.04) 0%, transparent 60%);
  pointer-events: none;
}

.card-herald::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(200, 150, 62, 0.15), transparent);
}

.herald-content {
  text-align: center;
  position: relative;
  z-index: 1;
}

.herald-emblem {
  width: 80px;
  height: 80px;
  margin: 0 auto var(--space-4);
  color: var(--color-brass);
  animation: emblem-pulse 4s ease-in-out infinite;
}

@keyframes emblem-pulse {
  0%, 100% { opacity: 0.8; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.03); }
}

.herald-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  margin-bottom: var(--space-4);
}

.herald-cb {
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 700;
  color: var(--color-brass);
  letter-spacing: 2px;
  line-height: 1;
}

.herald-dot {
  width: 6px;
  height: 6px;
  background: var(--color-brass);
  border-radius: 50%;
  margin-top: 8px;
}

.herald-title {
  font-family: var(--font-sans);
  font-size: var(--text-lg);
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  letter-spacing: 3px;
  margin-bottom: var(--space-1);
}

.herald-sub {
  font-family: var(--font-display);
  font-size: var(--text-sm);
  color: rgba(200, 150, 62, 0.6);
  line-height: 1.5;
  letter-spacing: 0.5px;
  font-style: italic;
  margin-bottom: var(--space-5);
}

.herald-line {
  width: 40px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--color-brass), transparent);
  margin: 0 auto var(--space-4);
}

.herald-tagline {
  font-size: var(--text-xs);
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 4px;
}

/* ── Right: Form panel ── */
.card-form {
  flex: 1;
  padding: var(--space-12) var(--space-10);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-header {
  margin-bottom: var(--space-8);
}

.form-title {
  font-family: var(--font-sans);
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--color-lead);
  margin-bottom: var(--space-1);
  letter-spacing: -0.3px;
}

.form-desc {
  font-size: var(--text-sm);
  color: var(--color-slate-light);
  letter-spacing: 0.2px;
}

/* ── Form fields ── */
.login-form :deep(.ant-form-item) {
  margin-bottom: var(--space-5);
}

.login-form :deep(.ant-input-affix-wrapper),
.login-form :deep(.ant-input-password) {
  border-radius: var(--radius-md);
  padding: 4px 12px;
  font-size: var(--text-sm);
}

.login-form :deep(.ant-input-prefix) {
  margin-right: 8px;
  color: var(--color-slate-light);
  display: flex;
  align-items: center;
}

.form-submit-item {
  margin-bottom: 0 !important;
  margin-top: var(--space-6);
}

.login-btn {
  height: 44px !important;
  font-size: var(--text-base) !important;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: var(--radius-md) !important;
  position: relative;
  overflow: hidden;
}

.login-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.1) 0%, transparent 50%);
  pointer-events: none;
}

.btn-arrow {
  transition: transform var(--duration-fast) var(--ease-out);
}

.login-btn:hover .btn-arrow {
  transform: translateX(3px);
}

/* ── Footer ── */
.form-footer {
  margin-top: var(--space-8);
  text-align: center;
}

.footer-line {
  display: block;
  width: 24px;
  height: 1px;
  background: rgba(11, 25, 44, 0.08);
  margin: 0 auto var(--space-3);
}

.footer-text {
  font-size: 11px;
  color: var(--color-slate-light);
  letter-spacing: 1px;
}

/* ── Responsive ── */
@media (max-width: 768px) {
  .login-card {
    flex-direction: column;
  }
  .card-herald {
    flex: 0 0 auto;
    padding: var(--space-6);
  }
  .card-herald::after {
    top: auto;
    bottom: 0;
    left: 0;
    right: 0;
    width: 100%;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(200,150,62,0.2), transparent);
  }
  .herald-emblem { width: 56px; height: 56px; }
  .herald-cb { font-size: 24px; }
  .card-form { padding: var(--space-8) var(--space-6); }
  .port-cities { display: none; }
  .trade-routes { opacity: 0.12; }
}
</style>
