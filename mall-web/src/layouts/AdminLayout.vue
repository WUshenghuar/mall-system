<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider v-model:collapsed="collapsed" collapsible>
      <div class="logo">跨境电商</div>
      <a-menu v-model:selectedKeys="selectedKeys" theme="dark" mode="inline" @click="handleMenuClick">
        <a-menu-item key="/dashboard">
          <dashboard-outlined /><span>首页</span>
        </a-menu-item>
        <a-menu-item key="/product">
          <shopping-outlined /><span>商品管理</span>
        </a-menu-item>
        <a-menu-item key="/order">
          <unordered-list-outlined /><span>订单管理</span>
        </a-menu-item>
        <a-menu-item key="/member">
          <user-outlined /><span>会员管理</span>
        </a-menu-item>
        <a-menu-item key="/marketing">
          <tag-outlined /><span>营销管理</span>
        </a-menu-item>
        <a-menu-item key="/finance">
          <dollar-outlined /><span>财务管理</span>
        </a-menu-item>
        <a-menu-item key="/system">
          <setting-outlined /><span>系统设置</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header style="background: #fff; padding: 0 24px">
        <div style="display: flex; justify-content: flex-end; align-items: center; height: 100%">
          <a-dropdown>
            <a class="ant-dropdown-link" @click.prevent>
              管理员 <down-outlined />
            </a>
            <template #overlay>
              <a-menu>
                <a-menu-item><a href="#">退出登录</a></a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>
      <a-layout-content style="margin: 24px; min-height: 280px">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const collapsed = ref(false)
const selectedKeys = ref(['/dashboard'])

function handleMenuClick({ key }) {
  router.push(key)
}
</script>

<style scoped>
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
}
</style>