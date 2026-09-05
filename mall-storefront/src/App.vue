<template>
  <main class="shell"><header class="app-header"><router-link to="/">海路集市</router-link><small>跨境好物 · 直达收货地</small></header>
    <router-view /><nav class="app-nav" aria-label="主导航"><router-link to="/">商品</router-link><router-link to="/cart">购物车</router-link><router-link to="/orders">订单</router-link><router-link to="/account">我的</router-link></nav>
  </main>
</template>
