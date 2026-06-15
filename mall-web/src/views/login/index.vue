<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="login-brand">
        <div class="brand-mark">CB</div>
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
            登录
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
const form = reactive({ username: '', password: '' })
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
  background:
    radial-gradient(ellipse at 30% 20%, rgba(200, 150, 62, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 70% 80%, rgba(11, 25, 44, 0.04) 0%, transparent 50%),
    var(--color-fog);
}

.login-panel {
  width: 400px;
  padding: var(--space-12) var(--space-10);
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-elevated);
}

.login-brand {
  text-align: center;
  margin-bottom: var(--space-10);
}

.brand-mark {
  width: 56px;
  height: 56px;
  margin: 0 auto var(--space-4);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-abyss);
  color: var(--color-brass);
  font-size: var(--text-xl);
  font-weight: 700;
  letter-spacing: -0.5px;
  border-radius: var(--radius-md);
}

.login-brand h1 {
  font-size: var(--text-2xl);
  color: var(--color-lead);
  font-weight: 600;
  letter-spacing: 1px;
}

.login-brand p {
  font-size: var(--text-sm);
  color: var(--color-slate-light);
  margin-top: var(--space-1);
  letter-spacing: 2px;
  text-transform: uppercase;
}

.login-form {
  margin-top: var(--space-4);
}
</style>
