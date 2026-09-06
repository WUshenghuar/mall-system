<template>
  <section class="page cart-page">
    <h2>装船清单</h2>
    <van-loading v-if="loading" />
    <div v-for="item in carts" :key="item.id" class="card cart-card">
      <van-checkbox v-model="item.checked" :true-value="1" :false-value="0" @change="save(item)">
        <span class="cart-sku">商品编号：{{ item.skuId }}</span>
      </van-checkbox>
      <div class="cart-actions">
        <van-stepper v-model="item.quantity" min="1" @change="save(item)" />
        <van-button plain type="danger" @click="remove(item.id)">移除</van-button>
      </div>
    </div>
    <van-empty v-if="!loading && !carts.length" description="购物车还是空的" />
    <van-button v-if="carts.length" block type="primary" class="checkout-button" @click="checkout">去结算</van-button>
  </section>
</template>
<script setup>
import {ref,onMounted} from 'vue';import{showToast}from'vant';import{tradeApi}from'../api';import{useRouter}from'vue-router'
const carts=ref([]),loading=ref(false),router=useRouter();async function load(){loading.value=true;try{carts.value=(await tradeApi.cart()).data||[]}catch(e){showToast(e)}finally{loading.value=false}}async function save(item){try{await tradeApi.updateCart(item.id,{quantity:item.quantity,checked:item.checked})}catch(e){showToast(e);load()}}async function remove(id){try{await tradeApi.removeCart(id);load()}catch(e){showToast(e)}}function checkout(){router.push('/checkout')}onMounted(load)
</script>
