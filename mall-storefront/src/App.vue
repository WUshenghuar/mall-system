<template>
  <main class="shell">
    <header class="app-header">
      <router-link to="/" class="brand">海路集市<small>跨境好物 · 直达收货地</small></router-link>
      <nav class="app-nav" aria-label="主导航">
        <router-link to="/">商品</router-link><router-link to="/promotions">优惠</router-link><router-link to="/cart">购物车</router-link><router-link to="/orders">订单</router-link><router-link to="/account">我的</router-link>
      </nav>
    </header>
    <div class="app-main"><router-view /></div>
    <CustomerServiceWidget />
  </main>
</template>

<script setup>
import CustomerServiceWidget from './components/CustomerServiceWidget.vue'
</script>
