<template>
  <div class="login-page">
    <!-- Decorative background -->
    <div class="bg-pattern"></div>

    <div class="login-panel">
      <div class="login-brand">
        <div class="brand-mark">
          <span class="brand-letters">CB</span>
          <div class="brand-dot"></div>
        </div>
        <h1>跨境电商管理平台</h1>
        <p>Cross-Border E-Commerce</p>
      </div>

      <a-form
        class="login-form"
        :model="form"
        @finish="handleLogin"
        autocomplete="off"
      >
        <a-form-item name="username" :rules="[{ required: true, message: '输入用户名' }]">
          <a-input
            v-model:value="form.username"
            placeholder="用户名"
            size="large"
          >
            <template #prefix><user-outlined /></template>
          </a-input>
        </a-form-item>

        <a-form-item name="password" :rules="[{ required: true, message: '输入密码' }]">
          <a-input-password
            v-model:value="form.password"
            placeholder="密码"
            size="large"
          >
            <template #prefix><lock-outlined /></template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            block
            :loading="loading"
          >
            登录后台
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
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
    // error handled by request interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-abyss);
  position: relative;
  overflow: hidden;
}

/* Brand pattern */
.bg-pattern {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 1200px 800px at 20% 50%, rgba(200, 150, 62, 0.07) 0%, transparent 60%),
    radial-gradient(ellipse 800px 600px at 80% 20%, rgba(59, 130, 246, 0.05) 0%, transparent 60%),
    radial-gradient(ellipse 600px 400px at 60% 80%, rgba(200, 150, 62, 0.04) 0%, transparent 50%);
  pointer-events: none;
}

.login-panel {
  position: relative;
  z-index: 1;
  width: 420px;
  padding: var(--space-12) var(--space-10);
  background: var(--color-surface);
  border-radius: 16px;
  box-shadow:
    0 4px 24px rgba(0,0,0,0.25),
    0 0 0 1px rgba(255,255,255,0.05) inset;
}

.login-brand {
  text-align: center;
  margin-bottom: var(--space-10);
}

.brand-mark {
  width: 64px;
  height: 64px;
  margin: 0 auto var(--space-4);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: var(--color-abyss);
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(11, 25, 44, 0.2);
}

.brand-letters {
  color: var(--color-brass);
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -1px;
}

.brand-dot {
  position: absolute;
  bottom: -3px;
  right: -3px;
  width: 10px;
  height: 10px;
  background: var(--color-brass);
  border-radius: 50%;
  border: 2px solid var(--color-surface);
}

.login-brand h1 {
  font-size: 22px;
  color: var(--color-lead);
  font-weight: 600;
  letter-spacing: 2px;
  margin-bottom: 4px;
}

.login-brand p {
  font-size: var(--text-xs);
  color: var(--color-slate-light);
  letter-spacing: 3px;
  text-transform: uppercase;
}

.login-form {
  margin-top: var(--space-4);
}

.login-form :deep(.ant-input-affix-wrapper),
.login-form :deep(.ant-input-password) {
  border-radius: var(--radius-md);
}
</style>
