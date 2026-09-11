<template>
  <section class="page product-detail-page">
    <van-nav-bar title="商品详情" left-text="返回" left-arrow @click-left="router.back" />
    <van-loading v-if="loading" class="page-loading" />
    <template v-else-if="product">
      <section class="product-detail-hero">
        <div class="detail-art">
          <img v-if="heroImage" :src="heroImage" :alt="spu.spuName" @error="imageBroken = true" />
          <span v-else>GLOBAL<br />SELECT</span>
        </div>
        <div class="detail-intro">
          <p class="eyebrow">IMPORTED PICK</p>
          <h1>{{ spu.spuName }}</h1>
          <p class="note">产地 {{ spu.originCountry || 'WORLDWIDE' }} · HS Code {{ spu.customsCode || '--' }}</p>
          <strong class="detail-price">{{ selectedSku?.currency || spu.currency || 'USD' }} {{ selectedSku?.price || spu.minPrice || '--' }}</strong>
          <p class="note">价格以所选规格为准，税费将在结算时按收货地计算。</p>
        </div>
      </section>
      <section class="card detail-card">
        <div class="section-heading"><h2>选择规格</h2><span class="note">{{ skus.length }} 个可选规格</span></div>
        <div class="sku-options">
          <button v-for="sku in skus" :key="sku.id" type="button" class="sku-option" :class="{ active: sku.id === selectedSkuId }" @click="selectedSkuId = sku.id">
            <b>{{ sku.skuCode }}</b><span>{{ sku.currency || 'USD' }} {{ sku.price }}</span>
          </button>
        </div>
      </section>
      <section v-if="descriptionText" class="card detail-card"><h2>商品说明</h2><p class="detail-description">{{ descriptionText }}</p></section>
      <div class="detail-actions"><van-button plain type="primary" @click="favorite">收藏</van-button><van-button type="primary" :loading="adding" :disabled="!selectedSku" @click="add">加入购物车</van-button></div>
    </template>
    <van-empty v-else description="商品不存在或已下架" />
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { memberApi, storeApi, tradeApi } from '../api'

const route = useRoute(), router = useRouter(), product = ref(null), loading = ref(true), adding = ref(false), selectedSkuId = ref(null), imageBroken = ref(false)
const spu = computed(() => product.value?.spu || {}), skus = computed(() => product.value?.skus || []), selectedSku = computed(() => skus.value.find(item => item.id === selectedSkuId.value) || skus.value[0]), heroImage = computed(() => imageBroken.value ? '' : spu.value.coverImage || selectedSku.value?.imageUrl || '')
const descriptionText = computed(() => { const value = spu.value.description; if (!value) return ''; if (typeof value !== 'string') return value.zh || value.en || ''; try { const parsed = JSON.parse(value); return parsed.zh || parsed.en || value } catch { return value } })

async function load() { try { product.value = (await storeApi.detail(route.params.id)).data; selectedSkuId.value = skus.value[0]?.id || null; if (localStorage.getItem('member-token')) memberApi.recordBrowse(route.params.id) } catch (error) { showToast(error) } finally { loading.value = false } }
async function favorite() { if (!localStorage.getItem('member-token')) { showToast('请先登录'); router.push('/account'); return }; try { await memberApi.addFavorite(spu.value.id); showToast('已收藏') } catch (error) { showToast(error) } }
async function add() { if (!localStorage.getItem('member-token')) { showToast('请先登录'); router.push('/account'); return }; if (!selectedSku.value || adding.value) return; adding.value = true; try { await tradeApi.addCart({ skuId: selectedSku.value.id, quantity: 1 }); showToast('已加入购物车') } catch (error) { showToast(error) } finally { adding.value = false } }
onMounted(load)
</script>

<style scoped>
.product-detail-page{padding-bottom:96px}.product-detail-hero{display:grid;gap:18px;margin:-16px -16px 20px;padding:16px;background:#f7fbfa}.detail-art{display:grid;min-height:260px;place-items:center;border-radius:16px;background:linear-gradient(135deg,#dceeed,#d8e2e8);color:var(--sea);font-size:30px;font-weight:900;letter-spacing:.06em;line-height:1.05}.detail-art img{width:100%;height:100%;min-height:260px;object-fit:cover;border-radius:16px}.detail-intro h1{margin:0;font-size:28px;line-height:1.25}.detail-intro .note{line-height:1.6}.detail-price{display:block;margin:20px 0 8px;color:var(--copper);font-size:28px}.detail-card h2{margin:0 0 14px;font-size:18px}.sku-options{display:grid;gap:8px}.sku-option{display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:48px;padding:0 14px;border:1px solid var(--line);border-radius:10px;background:#fff;color:var(--ink);font:inherit;text-align:left}.sku-option span{color:var(--copper);font-weight:700}.sku-option.active{border-color:var(--sea);background:#f1f9f8;box-shadow:0 0 0 2px rgba(12,93,117,.1)}.detail-description{margin:0;color:#587080;line-height:1.7;white-space:pre-wrap}.detail-actions{position:sticky;bottom:68px;display:flex;gap:10px;padding:10px 0;background:rgba(255,255,255,.94)}.detail-actions .van-button{flex:1;min-height:46px}@media(min-width:1025px){.product-detail-page{max-width:980px;margin:0 auto}.product-detail-hero{grid-template-columns:1fr 1fr;margin:-32px -32px 28px;padding:32px}.detail-actions{bottom:16px}}
</style>
