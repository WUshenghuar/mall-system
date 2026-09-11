<template>
  <section class="page refund-page">
    <van-nav-bar title="售后服务" left-text="返回" left-arrow @click-left="router.back" />
    <section v-if="orderNo" class="card refund-form">
      <h2>{{ refundType === 1 ? '申请退货退款' : '申请仅退款' }}</h2>
      <p class="note">订单号：{{ orderNo }}</p>
      <van-radio-group v-model="refundType" direction="horizontal" class="refund-types">
        <van-radio :name="0">仅退款</van-radio>
        <van-radio :name="1">退货退款</van-radio>
      </van-radio-group>
      <p v-if="refundType === 1" class="note">提交后请等待审核，通过后填写退货物流。</p>
      <van-field v-model="reason" label="申请原因" type="textarea" rows="3" maxlength="200" show-word-limit placeholder="请说明申请原因" />
      <div v-if="refundType === 1" class="evidence">
        <span class="field-label">凭证（最多 5 张）</span>
        <van-uploader multiple :max-count="5" :after-read="uploadEvidence" />
        <p v-if="evidenceUrls.length" class="note">已上传 {{ evidenceUrls.length }} 张凭证</p>
      </div>
      <van-button block type="warning" :loading="submitting" :disabled="!reason.trim() || uploading" @click="apply">提交申请</van-button>
    </section>

    <h2 class="section-title">售后进度</h2>
    <van-loading v-if="loading" class="page-loading" />
    <section v-for="item in refunds" :key="item.id" class="card refund-card">
      <div>
        <b>{{ item.orderNo }}</b>
        <p class="note">{{ typeLabel(item.refundType) }} · {{ item.refundReason }}</p>
      </div>
      <van-tag :type="tagType(item.refundStatus)">{{ label(item.refundStatus, item.refundType) }}</van-tag>
      <p class="money">退款金额：{{ item.refundAmount }}</p>
      <p v-if="item.evidenceUrls" class="note">已提交售后凭证</p>
      <p v-if="item.returnLogisticsNo" class="note">退货物流：{{ item.returnLogisticsCompany }} {{ item.returnLogisticsNo }}</p>
      <p v-if="item.approveComment" class="note">处理备注：{{ item.approveComment }}</p>
      <div v-if="item.refundType === 1 && item.refundStatus === 1" class="return-actions">
        <van-button v-if="returnId !== item.id" size="small" type="primary" plain @click="openReturn(item)">填写退货物流</van-button>
        <div v-else class="return-form">
          <van-field v-model="returnCompany" label="物流公司" placeholder="如 DHL" />
          <van-field v-model="returnTrackingNo" label="物流单号" placeholder="请输入退货单号" />
          <van-button block size="small" type="primary" :loading="returnSubmitting" :disabled="!returnCompany.trim() || !returnTrackingNo.trim()" @click="submitReturn(item)">提交物流</van-button>
        </div>
      </div>
    </section>
    <van-empty v-if="!loading && !refunds.length" description="暂时没有售后申请" />
  </section>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { fileApi, tradeApi } from '../api'

const route = useRoute(), router = useRouter()
const orderNo = ref(String(route.query.orderNo || '')), refundType = ref(Number(route.query.type || 0))
const reason = ref(''), evidenceUrls = ref([]), refunds = ref([]), loading = ref(false), submitting = ref(false), uploading = ref(false)
const returnId = ref(null), returnCompany = ref(''), returnTrackingNo = ref(''), returnSubmitting = ref(false)
let refreshTimer
const labels = ['待审核', '处理中', '已驳回', '已退款', '待平台收货']
const label = (status, type) => status === 1 && type === 1 ? '待用户退货' : labels[status] || '处理中'
const typeLabel = type => type === 1 ? '退货退款' : '仅退款'
const tagType = status => ['warning', 'primary', 'danger', 'success', 'primary'][status] || 'default'

async function load() {
  loading.value = true
  try { refunds.value = (await tradeApi.refunds({ page: 1, size: 20 })).data.records || [] } catch (e) { showToast(e) } finally { loading.value = false }
}
async function uploadEvidence(files) {
  uploading.value = true
  try {
    for (const item of (Array.isArray(files) ? files : [files])) {
      const result = await fileApi.upload(item.file)
      const url = result.data?.url || result.data?.path
      if (url) evidenceUrls.value.push(url)
    }
  } catch (e) { showToast(e) } finally { uploading.value = false }
}
async function apply() {
  submitting.value = true
  try {
    await tradeApi.applyRefund({ orderNo: orderNo.value, reason: reason.value.trim(), refundType: refundType.value, evidenceUrls: evidenceUrls.value })
    showToast('售后申请已提交'); reason.value = ''; evidenceUrls.value = []; orderNo.value = ''; await load()
  } catch (e) { showToast(e) } finally { submitting.value = false }
}
function openReturn(item) { returnId.value = item.id; returnCompany.value = ''; returnTrackingNo.value = '' }
async function submitReturn(item) {
  returnSubmitting.value = true
  try { await tradeApi.submitReturn(item.id, { company: returnCompany.value.trim(), trackingNo: returnTrackingNo.value.trim() }); showToast('退货物流已提交'); returnId.value = null; await load() } catch (e) { showToast(e) } finally { returnSubmitting.value = false }
}
onMounted(() => { load(); refreshTimer = window.setInterval(load, 15000) })
onUnmounted(() => window.clearInterval(refreshTimer))
</script>

<style scoped>
.refund-page { padding-bottom: 88px; }.refund-form, .refund-card { margin-bottom: 12px; }.refund-form h2, .section-title { margin: 4px 0 12px; }.refund-types { margin: 12px 0; }.evidence { padding: 8px 16px 16px; }.field-label { display: block; margin-bottom: 8px; color: var(--color-ink, #172b4d); font-size: 14px; }.refund-card { display: grid; grid-template-columns: 1fr auto; gap: 8px; }.refund-card .money, .refund-card .note:last-child, .return-actions { grid-column: 1 / -1; margin: 0; }.return-form { margin-top: 8px; }.page-loading { display: block; margin: 32px auto; }
@media (min-width: 1025px){ .refund-page { padding-bottom: 24px; } }
</style>
