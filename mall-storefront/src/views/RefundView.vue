<template>
  <section class="page refund-page">
    <van-nav-bar title="退款售后" left-text="返回" left-arrow @click-left="router.back" />
    <section v-if="orderNo" class="card refund-form"><h2>申请仅退款</h2><p class="note">订单号：{{ orderNo }}</p><van-field v-model="reason" label="退款原因" type="textarea" rows="3" maxlength="200" show-word-limit placeholder="请说明退款原因" /><van-button block type="warning" :loading="submitting" :disabled="!reason.trim()" @click="apply">提交申请</van-button></section>
    <h2 class="section-title">退款进度</h2><van-loading v-if="loading" class="page-loading" /><section v-for="item in refunds" :key="item.id" class="card refund-card"><div><b>{{ item.orderNo }}</b><p class="note">{{ item.refundReason }}</p></div><van-tag :type="tagType(item.refundStatus)">{{ label(item.refundStatus) }}</van-tag><p class="money">{{ item.refundAmount }}</p><p v-if="item.approveComment" class="note">处理备注：{{ item.approveComment }}</p></section><van-empty v-if="!loading && !refunds.length" description="暂时没有退款申请" />
  </section>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { tradeApi } from '../api'
const route = useRoute(), router = useRouter(), orderNo = ref(route.query.orderNo || ''), reason = ref(''), refunds = ref([]), loading = ref(false), submitting = ref(false)
const labels = ['待审核', '审核通过', '已驳回', '已退款']; const label = status => labels[status] || '处理中'; const tagType = status => ['warning', 'primary', 'danger', 'success'][status] || 'default'
async function load() { loading.value = true; try { refunds.value = (await tradeApi.refunds({ page: 1, size: 20 })).data.records || [] } catch (e) { showToast(e) } finally { loading.value = false } }
async function apply() { submitting.value = true; try { await tradeApi.applyRefund({ orderNo: orderNo.value, reason: reason.value.trim() }); showToast('退款申请已提交'); reason.value = ''; orderNo.value = ''; await load() } catch (e) { showToast(e) } finally { submitting.value = false } }
onMounted(load)
</script>
<style scoped>
.refund-page { padding-bottom: 88px; }.refund-form, .refund-card { margin-bottom: 12px; }.refund-form h2, .section-title { margin: 4px 0 12px; }.refund-card { display: grid; grid-template-columns: 1fr auto; gap: 8px; }.refund-card .money, .refund-card .note:last-child { grid-column: 1 / -1; margin: 0; }.page-loading { display: block; margin: 32px auto; }
</style>
