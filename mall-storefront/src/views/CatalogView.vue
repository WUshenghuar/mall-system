<template>
  <section class="page catalog-page">
    <header class="catalog-hero">
      <div>
        <p class="eyebrow">CURATED WORLDWIDE</p>
        <h1>把世界好物，带回家。</h1>
        <p>从可信产地挑选，直达你的收货地。</p>
      </div>
      <span class="hero-mark" aria-hidden="true">CB</span>
    </header>
    <div class="catalog-toolbar">
      <label class="search-box">
        <span class="sr-only">搜索商品</span>
        <input v-model="keyword" placeholder="搜索商品" aria-label="搜索商品" @keyup.enter="load" />
      </label>
      <van-button type="primary" @click="load">搜索</van-button>
    </div>
    <van-loading v-if="loading" class="page-loading" />
    <div class="product-grid">
      <article
        v-for="item in products"
        :key="item.id"
        class="product"
        role="button"
        tabindex="0"
        @click="open(item)"
        @keyup.enter="open(item)"
        @keyup.space.prevent="open(item)"
      >
        <div class="product-art" :class="{ 'has-image': item.coverImage }">
          <img v-if="item.coverImage" :src="item.coverImage" :alt="item.spuName" loading="lazy" @error="item.coverImage = ''" />
          <template v-else><span>GLOBAL<br />SELECT</span><small>{{ item.originCountry || 'WORLDWIDE' }}</small></template>
          <span v-if="item.coverImage" class="product-origin">{{ item.originCountry || 'WORLDWIDE' }}</span>
        </div>
        <div class="product-copy">
          <p class="eyebrow">IMPORTED PICK</p>
          <h3>{{ item.spuName }}</h3>
          <p class="note">{{ item.originCountry || '跨境精选' }} · 已售 {{ item.salesCount || 0 }}</p>
          <div class="product-footer">
            <p v-if="item.minPrice != null" class="money">{{ item.currency || 'USD' }} {{ item.minPrice }}</p>
            <p v-else class="money">多规格可选</p>
            <van-button size="small" plain type="primary" @click.stop="open(item)">查看规格</van-button>
          </div>
        </div>
      </article>
    </div>
    <section v-if="!loading && loadError" class="network-state"><van-empty image="network" description="暂时无法连接商品服务" /><p>请确认后端服务已启动后重试。</p><van-button type="primary" @click="load">重新连接</van-button></section>
    <van-empty v-else-if="!loading && !products.length" description="暂时没有匹配商品" />
    <van-action-sheet v-model:show="show" :title="detail?.spu?.spuName"><div class="page"><van-button block plain @click="favorite">收藏此商品</van-button></div><div class="page" v-for="sku in detail?.skus" :key="sku.id"><b>{{ sku.skuCode }}</b><p class="money">{{ sku.currency }} {{ sku.price }}</p><van-button block type="primary" @click="add(sku.id)">加入购物车</van-button></div></van-action-sheet>
  </section>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { showToast } from 'vant'
import { storeApi, tradeApi, memberApi } from '../api'
const products=ref([]),keyword=ref(''),loading=ref(false),show=ref(false),detail=ref(null),loadError=ref('')
async function load(){loading.value=true;loadError.value='';try{products.value=(await storeApi.products({keyword:keyword.value})).data.records||[]}catch(e){products.value=[];loadError.value=String(e)}finally{loading.value=false}}
async function open(item){try{detail.value=(await storeApi.detail(item.id)).data;show.value=true;if(localStorage.getItem('member-token'))await memberApi.recordBrowse(item.id)}catch(e){showToast(e)}}
async function favorite(){try{await memberApi.addFavorite(detail.value.spu.id);showToast('已收藏')}catch(e){showToast(e)}}
async function add(skuId){try{await tradeApi.addCart({skuId,quantity:1});showToast('已加入购物车')}catch(e){showToast(e)}}
onMounted(load)
</script>
