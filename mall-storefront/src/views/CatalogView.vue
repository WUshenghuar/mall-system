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
      <van-button v-if="keyword || categoryId !== null || minPrice !== '' || maxPrice !== '' || sortField !== 'sales'" plain @click="resetFilters">清除</van-button>
    </div>
    <nav class="catalog-filters" aria-label="商品分类">
      <button type="button" :class="{ active: categoryId === null }" @click="selectCategory(null)">全部</button>
      <button v-for="category in categories" :key="category.id" type="button" :class="{ active: categoryId === category.id }" @click="selectCategory(category.id)">
        {{ category.categoryName }}
      </button>
    </nav>
    <div class="catalog-refine">
      <label>最低价 <input v-model="minPrice" type="number" min="0" step="0.01" placeholder="不限" @keyup.enter="load" /></label>
      <label>最高价 <input v-model="maxPrice" type="number" min="0" step="0.01" placeholder="不限" @keyup.enter="load" /></label>
      <label>排序
        <select v-model="sortField" aria-label="商品排序" @change="load">
          <option value="sales">按热度</option>
          <option value="newest">最新上架</option>
          <option value="priceAsc">价格从低到高</option>
          <option value="priceDesc">价格从高到低</option>
        </select>
      </label>
      <van-button plain type="primary" @click="load">应用</van-button>
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
  </section>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { storeApi } from '../api'
const router=useRouter(),products=ref([]),categories=ref([]),keyword=ref(''),categoryId=ref(null),minPrice=ref(''),maxPrice=ref(''),sortField=ref('sales'),loading=ref(false),loadError=ref('')
async function load(){if(minPrice.value !== '' && maxPrice.value !== '' && Number(minPrice.value)>Number(maxPrice.value)){showToast('最低价不能高于最高价');return};loading.value=true;loadError.value='';const params={keyword:keyword.value,categoryId:categoryId.value ?? undefined,minPrice:minPrice.value || undefined,maxPrice:maxPrice.value || undefined,sortField:sortField.value};try{let result;if(keyword.value.trim()){try{result=await storeApi.search(params)}catch{result=await storeApi.products(params)}}else{result=await storeApi.products(params)};products.value=(result.data.records||[]).map(item=>({...item,id:item.id ?? item.spuId}))}catch(e){products.value=[];loadError.value=String(e)}finally{loading.value=false}}
async function loadCategories(){try{categories.value=(await storeApi.categories()).data||[]}catch{categories.value=[]}}
function selectCategory(id){categoryId.value=id;load()}
function resetFilters(){keyword.value='';categoryId.value=null;minPrice.value='';maxPrice.value='';sortField.value='sales';load()}
function open(item){router.push(`/products/${item.id}`)}
onMounted(() => { loadCategories(); load() })
</script>
