<template>
  <div>
    <a-row :gutter="16">
      <a-col :span="6" v-for="card in stats" :key="card.title">
        <a-card style="margin-bottom: 16px">
          <a-statistic :title="card.title" :value="card.value" :prefix="card.icon" />
        </a-card>
      </a-col>
    </a-row>
    <a-card title="欢迎">
      <p>欢迎使用跨境电商后台管理系统</p>
    </a-card>
  </div>
</template>

<script setup>
const stats = [
  { title: '商品总数', value: 0, icon: '📦' },
  { title: '今日订单', value: 0, icon: '📋' },
  { title: '会员总数', value: 0, icon: '👤' },
  { title: '待处理退款', value: 0, icon: '💰' }
]
</script>