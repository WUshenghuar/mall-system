<template>
  <div class="sub-page support-page">
    <div class="page-head"><div><h2 class="page-title">平台客服工单</h2><p class="page-desc">处理 C 端 AI 客服转交的会员问题</p></div></div>
    <a-card :bordered="false">
      <a-tabs v-model:activeKey="statusFilter" @change="fetchData">
        <a-tab-pane key="" tab="全部" /><a-tab-pane key="0" tab="待接管" /><a-tab-pane key="1" tab="处理中" /><a-tab-pane key="2" tab="已解决" />
      </a-tabs>
      <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id" :pagination="pagination" @change="onPage">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'"><a-tag v-if="record.status === 0" color="orange">待接管</a-tag><a-tag v-else-if="record.status === 1" color="blue">处理中</a-tag><a-tag v-else color="green">已解决</a-tag></template>
          <template v-if="column.key === 'message'"><span class="message-cell">{{ record.latestMessage }}</span></template>
          <template v-if="column.key === 'action'"><a-space v-if="record.status === 0"><a-button size="small" type="primary" @click="claim(record.id)">认领</a-button></a-space><a-space v-else-if="record.status === 1"><a-button size="small" type="primary" @click="openResolve(record)">标记已解决</a-button></a-space><span v-else>--</span></template>
        </template>
      </a-table>
    </a-card>
    <a-modal v-model:open="resolveOpen" title="完成客服工单" :confirm-loading="resolving" ok-text="标记已解决" @ok="resolve">
      <a-textarea v-model:value="resolveNote" :rows="4" placeholder="填写处理结果（可选）" />
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { claimSupportTicket, getSupportTicketPage, resolveSupportTicket } from '@/api/ai'

const loading = ref(false), list = ref([]), statusFilter = ref('')
const pagination = ref({ current: 1, pageSize: 10, total: 0 })
const resolveOpen = ref(false), resolving = ref(false), resolveTarget = ref(null), resolveNote = ref('')
const columns = [
  { title: '工单号', dataIndex: 'ticketNo', width: 190 }, { title: '会员 ID', dataIndex: 'memberId', width: 90 },
  { title: '问题摘要', dataIndex: 'subject', width: 180, ellipsis: true }, { title: '最新留言', key: 'message', ellipsis: true },
  { title: '状态', key: 'status', width: 90 }, { title: '操作', key: 'action', width: 130 }
]
async function fetchData() { loading.value = true; try { const res = await getSupportTicketPage({ page: pagination.value.current, size: pagination.value.pageSize, status: statusFilter.value || undefined }); list.value = res.data?.records || []; pagination.value.total = res.data?.total || 0 } finally { loading.value = false } }
function onPage(p) { pagination.value.current = p.current; fetchData() }
async function claim(id) { try { await claimSupportTicket(id); message.success('工单已认领'); fetchData() } catch { /* interceptor shows error */ } }
function openResolve(record) { resolveTarget.value = record; resolveNote.value = ''; resolveOpen.value = true }
async function resolve() { if (!resolveTarget.value) return; resolving.value = true; try { await resolveSupportTicket(resolveTarget.value.id, resolveNote.value.trim()); message.success('工单已解决'); resolveOpen.value = false; fetchData() } catch { /* interceptor shows error */ } finally { resolving.value = false } }
onMounted(fetchData)
</script>

<style scoped>
.support-page { max-width: 1200px; }
.message-cell { display: block; max-width: 360px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
