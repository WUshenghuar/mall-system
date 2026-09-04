<template><section class="page"><div class="bar"><input v-model="keyword" placeholder="搜索商品" @keyup.enter="load"/><van-button type="primary" @click="load">搜索</van-button></div>
  <van-loading v-if="loading" /> <article v-for="item in products" :key="item.id" class="product" @click="open(item)"><h3>{{ item.spuName }}</h3><p class="note">{{ item.originCountry || '跨境精选' }} · 已售 {{ item.salesCount || 0 }}</p><van-button size="small" plain type="primary">查看规格</van-button></article>
  <van-empty v-if="!loading && !products.length" description="暂时没有匹配商品" />
  <van-action-sheet v-model:show="show" :title="detail?.spu?.spuName"><div class="page"><van-button block plain @click="favorite">收藏此商品</van-button></div><div class="page" v-for="sku in detail?.skus" :key="sku.id"><b>{{ sku.skuCode }}</b><p class="money">{{ sku.currency }} {{ sku.price }}</p><van-button block type="primary" @click="add(sku.id)">加入购物车</van-button></div></van-action-sheet>
</section></template>
<script setup>
import { ref, onMounted } from 'vue'
import { showToast } from 'vant'
import { storeApi, tradeApi, memberApi } from '../api'
const products=ref([]),keyword=ref(''),loading=ref(false),show=ref(false),detail=ref(null)
async function load(){loading.value=true;try{products.value=(await storeApi.products({keyword:keyword.value})).data.records||[]}catch(e){showToast(e)}finally{loading.value=false}}
async function open(item){try{detail.value=(await storeApi.detail(item.id)).data;show.value=true;if(localStorage.getItem('member-token'))await memberApi.recordBrowse(item.id)}catch(e){showToast(e)}}
async function favorite(){try{await memberApi.addFavorite(detail.value.spu.id);showToast('已收藏')}catch(e){showToast(e)}}
async function add(skuId){try{await tradeApi.addCart({skuId,quantity:1});showToast('已加入购物车')}catch(e){showToast(e)}}
onMounted(load)
</script>
