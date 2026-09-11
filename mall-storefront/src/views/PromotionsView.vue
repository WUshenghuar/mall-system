<template>
  <section class="page promotions-page">
    <header class="promotion-hero"><p class="eyebrow">SEASONAL EDITS</p><h1>本周跨境好价</h1><p>限时优惠已为你准备好，挑选好物后即可结算。</p></header>
    <section><div class="section-heading"><h2>可领取优惠券</h2><span class="note">登录后领取</span></div>
      <van-loading v-if="loading" class="page-loading" />
      <article v-for="coupon in coupons" :key="coupon.id" class="coupon-card" :class="{ 'coupon-claimed': claimedCouponIds.has(coupon.id) }"><div><p class="coupon-kicker">{{ coupon.couponType }}</p><strong>{{ discountText(coupon) }}</strong><p>{{ coupon.couponName }}</p><small>有效至 {{ formatTime(coupon.validEnd) }}</small></div><van-button type="primary" size="small" :loading="claiming === coupon.id" :disabled="claimedCouponIds.has(coupon.id)" @click="claim(coupon.id)">{{ claimedCouponIds.has(coupon.id) ? '已领取' : '领取' }}</van-button></article>
      <van-empty v-if="!loading && !coupons.length" description="暂时没有可领取优惠券" />
    </section>
    <section><div class="section-heading"><h2>正在进行的活动</h2></div>
      <article v-for="activity in activities" :key="activity.id" class="campaign-card"><p class="eyebrow">{{ activity.activityType }}</p><h3>{{ activity.activityName }}</h3><p class="note">截止 {{ formatTime(activity.endTime) }}</p><div v-if="activity.skuItems?.length" class="campaign-skus"><button v-for="item in activity.skuItems" :key="item.skuId" type="button" class="campaign-sku-link" :disabled="!item.spuId" @click.stop="openProduct(item.spuId)">SKU #{{ item.skuId }}<b v-if="item.seckillPrice"> · 活动价 {{ item.seckillPrice }}</b></button></div><van-button plain type="primary" size="small" @click="openActivity(activity)">{{ activity.skuItems?.some(item => item.spuId) ? '查看活动商品' : '去挑选商品' }}</van-button></article>
      <van-empty v-if="!loading && !activities.length" description="新品活动即将上线" />
    </section>
  </section>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { marketingApi } from '../api'
const router = useRouter(), coupons = ref([]), activities = ref([]), loading = ref(false), claiming = ref(null), claimedCouponIds = ref(new Set())
const formatTime = value => value?.replace('T', ' ').slice(0, 16) || '--'
const discountText = coupon => coupon.couponType === 'DISCOUNT' ? `${coupon.discount} 折` : `¥${coupon.discount}`
function openProduct(spuId) { if (spuId) router.push(`/products/${spuId}`) }
function openActivity(activity) { const spuId = activity.skuItems?.find(item => item.spuId)?.spuId; if (spuId) openProduct(spuId); else router.push('/') }
async function load() { loading.value = true; try { const requests = [marketingApi.coupons(), marketingApi.activities()]; if (localStorage.getItem('member-token')) requests.push(marketingApi.memberCoupons()); const results = await Promise.all(requests); coupons.value = results[0].data || []; activities.value = results[1].data || []; claimedCouponIds.value = new Set((results[2]?.data || []).map(coupon => coupon.couponId)) } catch (e) { showToast(e) } finally { loading.value = false } }
async function claim(id) { if (!localStorage.getItem('member-token')) { showToast('请先登录后领取'); router.push('/account'); return }; claiming.value = id; try { await marketingApi.claimCoupon(id); claimedCouponIds.value = new Set([...claimedCouponIds.value, id]); showToast('优惠券已放入账户') } catch (e) { showToast(e) } finally { claiming.value = null } }
onMounted(load)
</script>
<style scoped>
.promotions-page{padding-bottom:88px}.promotion-hero{margin:-16px -16px 24px;padding:30px 20px;background:linear-gradient(135deg,#102a43,#0c5d75);color:#fff}.promotion-hero .eyebrow{color:#d6edf2}.promotion-hero h1{margin:0;font-size:30px;letter-spacing:-.04em}.promotion-hero p:last-child{margin:10px 0 0;color:#d6edf2;line-height:1.6}.coupon-card,.campaign-card{display:flex;align-items:center;justify-content:space-between;gap:16px;margin:0 0 12px;padding:16px;border:1px solid var(--line);border-radius:14px;background:#fff}.coupon-card>div{min-width:0}.coupon-card strong{display:block;color:var(--copper);font-size:25px}.coupon-card p{margin:4px 0;color:var(--ink);font-weight:700}.coupon-card small{color:#64748b}.coupon-kicker{font-size:11px!important;letter-spacing:.1em;color:var(--sea)!important}.coupon-card.coupon-claimed{background:#f3f4f6;border-color:#e2e5e9;filter:grayscale(1)}.coupon-card.coupon-claimed strong,.coupon-card.coupon-claimed p,.coupon-card.coupon-claimed small,.coupon-card.coupon-claimed .coupon-kicker{color:#8a96a3!important}.coupon-card.coupon-claimed .van-button{background:#c5cbd2;border-color:#c5cbd2;color:#fff}.campaign-card{display:block;background:linear-gradient(135deg,#f7fbfa,#fff8f1)}.campaign-card h3{margin:4px 0 8px;font-size:19px}.campaign-skus{display:flex;flex-wrap:wrap;gap:6px;margin:0 0 4px;color:#64748b;font-size:12px}.campaign-skus span{padding:4px 8px;border-radius:999px;background:#fff}.campaign-skus b{color:var(--copper)}.campaign-card .van-button{margin-top:8px}@media(min-width:1025px){.promotions-page{max-width:980px;margin:0 auto;padding-bottom:24px}.promotion-hero{margin:-32px -32px 28px;padding:42px 36px;border-radius:0 0 20px 20px}.coupon-card{width:calc(50% - 8px);display:inline-flex;vertical-align:top;margin-right:12px}.coupon-card:nth-of-type(even){margin-right:0}}
.campaign-sku-link{padding:4px 8px;border:0;border-radius:999px;background:#fff;color:#64748b;font:inherit;font-size:12px;cursor:pointer}.campaign-sku-link:not(:disabled):hover{color:var(--sea)}.campaign-sku-link:focus-visible{outline:3px solid rgba(12,93,117,.2);outline-offset:2px}.campaign-sku-link:disabled{cursor:default}
</style>
