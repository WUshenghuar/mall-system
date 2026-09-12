<template>
  <div class="sub-page support-page">
    <div class="page-head"><div><h2 class="page-title">平台客服工单</h2><p class="page-desc">处理 C 端 AI 客服转交的会员问题</p></div><a-space class="feedback-stats"><a-button @click="openKnowledge">AI 知识库</a-button><a-statistic title="已评价" :value="feedbackStats.total" /><a-statistic title="有帮助率" :value="helpfulRate" suffix="%" /></a-space></div>
    <a-card :bordered="false">
      <a-tabs v-model:activeKey="statusFilter" @change="fetchData">
        <a-tab-pane key="" tab="全部" /><a-tab-pane key="0" tab="待接管" /><a-tab-pane key="1" tab="处理中" /><a-tab-pane key="2" tab="已解决" />
      </a-tabs>
      <a-table :columns="columns" :data-source="list" :loading="loading" row-key="id" :pagination="pagination" @change="onPage">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'"><a-tag v-if="record.status === 0" color="orange">待接管</a-tag><a-tag v-else-if="record.status === 1" color="blue">处理中</a-tag><a-tag v-else color="green">已解决</a-tag></template>
          <template v-if="column.key === 'message'"><span class="message-cell">{{ record.latestMessage }}</span></template>
          <template v-if="column.key === 'reply'"><span class="message-cell">{{ record.agentReply || '--' }}</span></template>
          <template v-if="column.key === 'action'"><a-space v-if="record.status === 0"><a-button size="small" type="primary" @click="claim(record.id)">认领</a-button><a-button size="small" @click="openConversation(record)">会话</a-button></a-space><a-space v-else-if="record.status === 1"><a-button size="small" type="primary" @click="openReply(record)">回复</a-button><a-button size="small" @click="openResolve(record)">标记已解决</a-button><a-button size="small" @click="openConversation(record)">会话</a-button></a-space><a-button v-else size="small" @click="openConversation(record)">会话</a-button></template>
        </template>
      </a-table>
    </a-card>
    <a-modal v-model:open="knowledgeOpen" title="AI 客服知识库" :footer="null" width="760px">
      <a-spin :spinning="knowledgeLoading">
        <div class="knowledge-toolbar"><span>保存后立即用于下一次问答</span><a-button type="primary" @click="newKnowledge">新增条目</a-button></div>
        <a-empty v-if="!knowledgeDocs.length" description="暂无知识条目" />
        <div v-for="item in knowledgeDocs" :key="item.id" class="knowledge-item">
          <div><div class="knowledge-title"><strong>{{ item.title }}</strong><a-tag>{{ item.category }}</a-tag></div><p>{{ item.content }}</p></div>
          <a-button size="small" @click="editKnowledge(item)">编辑</a-button>
        </div>
      </a-spin>
    </a-modal>
    <a-modal v-model:open="knowledgeEditorOpen" :title="knowledgeEditingId ? '编辑知识条目' : '新增知识条目'" :confirm-loading="knowledgeSaving" ok-text="保存" @ok="saveKnowledge">
      <a-form layout="vertical">
        <a-form-item label="标识"><a-input v-model:value="knowledgeForm.id" :disabled="Boolean(knowledgeEditingId)" placeholder="留空自动生成，如 refund-policy" /></a-form-item>
        <a-form-item label="标题" required><a-input v-model:value="knowledgeForm.title" maxlength="120" /></a-form-item>
        <a-form-item label="分类" required><a-input v-model:value="knowledgeForm.category" maxlength="64" placeholder="如 after_sales、marketing" /></a-form-item>
        <a-form-item label="内容" required><a-textarea v-model:value="knowledgeForm.content" :rows="6" maxlength="5000" show-count /></a-form-item>
      </a-form>
    </a-modal>
    <a-modal v-model:open="conversationOpen" title="AI 会话记录" :footer="null" width="600px">
      <a-spin :spinning="conversationLoading">
        <a-empty v-if="!conversationMessages.length" description="暂无会话记录" />
        <div v-for="item in conversationMessages" :key="item.id" class="conversation-item" :class="item.role">
          <a-tag :color="item.role === 'user' ? 'blue' : 'default'">{{ item.role === 'user' ? '会员' : 'AI 客服' }}</a-tag>
          <span>{{ item.content }}</span>
        </div>
      </a-spin>
    </a-modal>
    <a-modal v-model:open="replyOpen" title="回复会员" :confirm-loading="replying" ok-text="发送回复" @ok="reply">
      <a-textarea v-model:value="replyContent" :rows="4" maxlength="1000" show-count placeholder="输入给会员的处理进展或说明" />
    </a-modal>
    <a-modal v-model:open="resolveOpen" title="完成客服工单" :confirm-loading="resolving" ok-text="标记已解决" @ok="resolve">
      <a-textarea v-model:value="resolveNote" :rows="4" placeholder="填写处理结果（可选）" />
    </a-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { claimSupportTicket, getAiFeedbackStats, getAiKnowledge, getSupportTicketConversation, getSupportTicketPage, replySupportTicket, resolveSupportTicket, saveAiKnowledge } from '@/api/ai'

const loading = ref(false), list = ref([]), statusFilter = ref('')
const pagination = ref({ current: 1, pageSize: 10, total: 0 })
const replyOpen = ref(false), replying = ref(false), replyTarget = ref(null), replyContent = ref('')
const conversationOpen = ref(false), conversationLoading = ref(false), conversationMessages = ref([])
const feedbackStats = ref({ total: 0, positive: 0, negative: 0 })
const knowledgeOpen = ref(false), knowledgeLoading = ref(false), knowledgeSaving = ref(false), knowledgeEditorOpen = ref(false), knowledgeDocs = ref([]), knowledgeEditingId = ref('')
const knowledgeForm = ref({ id: '', title: '', category: '', content: '' })
const resolveOpen = ref(false), resolving = ref(false), resolveTarget = ref(null), resolveNote = ref('')
let refreshTimer
const helpfulRate = computed(() => { const total = Number(feedbackStats.value.total) || 0; return total ? Math.round((Number(feedbackStats.value.positive) || 0) * 100 / total) : 0 })
const columns = [
  { title: '工单号', dataIndex: 'ticketNo', width: 190 }, { title: '会员 ID', dataIndex: 'memberId', width: 90 },
  { title: '问题摘要', dataIndex: 'subject', width: 180, ellipsis: true }, { title: '最新留言', key: 'message', ellipsis: true },
  { title: '客服回复', key: 'reply', ellipsis: true }, { title: '状态', key: 'status', width: 90 }, { title: '操作', key: 'action', width: 190 }
]
async function fetchData() { loading.value = true; try { const res = await getSupportTicketPage({ page: pagination.value.current, size: pagination.value.pageSize, status: statusFilter.value || undefined }); list.value = res.data?.records || []; pagination.value.total = res.data?.total || 0 } finally { loading.value = false } }
async function fetchFeedbackStats() { try { feedbackStats.value = (await getAiFeedbackStats()).data || feedbackStats.value } catch { /* 指标失败不影响工单处理 */ } }
async function openKnowledge() { knowledgeOpen.value = true; knowledgeLoading.value = true; try { knowledgeDocs.value = (await getAiKnowledge()).data || [] } catch { message.error('知识库读取失败') } finally { knowledgeLoading.value = false } }
function newKnowledge() { knowledgeEditingId.value = ''; knowledgeForm.value = { id: '', title: '', category: '', content: '' }; knowledgeEditorOpen.value = true }
function editKnowledge(item) { knowledgeEditingId.value = item.id; knowledgeForm.value = { ...item }; knowledgeEditorOpen.value = true }
async function saveKnowledge() { const form = knowledgeForm.value; if (!form.title.trim() || !form.category.trim() || !form.content.trim()) { message.warning('请填写完整知识条目'); return }; knowledgeSaving.value = true; try { await saveAiKnowledge({ ...form, id: form.id.trim() }); message.success('知识条目已保存'); knowledgeEditorOpen.value = false; openKnowledge() } catch { /* interceptor shows error */ } finally { knowledgeSaving.value = false } }
function onPage(p) { pagination.value.current = p.current; fetchData() }
async function claim(id) { try { await claimSupportTicket(id); message.success('工单已认领'); fetchData() } catch { /* interceptor shows error */ } }
async function openConversation(record) { conversationOpen.value = true; conversationMessages.value = []; conversationLoading.value = true; try { conversationMessages.value = (await getSupportTicketConversation(record.id)).data || [] } catch { message.error('会话读取失败') } finally { conversationLoading.value = false } }
function openReply(record) { replyTarget.value = record; replyContent.value = ''; replyOpen.value = true }
async function reply() { if (!replyTarget.value || !replyContent.value.trim()) { message.warning('请输入回复内容'); return }; replying.value = true; try { await replySupportTicket(replyTarget.value.id, replyContent.value.trim()); message.success('已发送回复'); replyOpen.value = false; fetchData() } catch { /* interceptor shows error */ } finally { replying.value = false } }
function openResolve(record) { resolveTarget.value = record; resolveNote.value = ''; resolveOpen.value = true }
async function resolve() { if (!resolveTarget.value) return; resolving.value = true; try { await resolveSupportTicket(resolveTarget.value.id, resolveNote.value.trim()); message.success('工单已解决'); resolveOpen.value = false; fetchData() } catch { /* interceptor shows error */ } finally { resolving.value = false } }
onMounted(() => { fetchData(); fetchFeedbackStats(); refreshTimer = window.setInterval(() => { fetchData(); fetchFeedbackStats() }, 15000) })
onUnmounted(() => window.clearInterval(refreshTimer))
</script>

<style scoped>
.support-page { max-width: 1200px; }
.feedback-stats :deep(.ant-statistic) { min-width: 92px; }
.knowledge-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; color: #64748b; }
.knowledge-item { display: flex; gap: 16px; align-items: flex-start; justify-content: space-between; padding: 14px 0; border-bottom: 1px solid #f0f0f0; }
.knowledge-title { display: flex; gap: 8px; align-items: center; }
.knowledge-item p { max-width: 580px; margin: 6px 0 0; color: #64748b; line-height: 1.6; white-space: pre-wrap; }
.message-cell { display: block; max-width: 360px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conversation-item { display: flex; gap: 10px; align-items: flex-start; padding: 10px 0; border-bottom: 1px solid #f0f0f0; line-height: 1.6; }
.conversation-item span { white-space: pre-wrap; overflow-wrap: anywhere; }
</style>
