<template>
  <div class="customer-service">
    <van-button round type="primary" class="service-trigger" aria-label="打开平台客服" @click="open = true">客服</van-button>
    <van-popup v-model:show="open" position="bottom" round :style="{ height: '72%' }">
      <section class="service-panel" aria-label="平台智能客服">
        <header><div><p class="eyebrow">PLATFORM SUPPORT</p><h2>海路客服</h2><small v-if="handoffTicket">工单 {{ handoffTicket.ticketNo }} · {{ handoffStatus }}</small></div><div class="service-header-actions"><van-button plain size="small" type="warning" :disabled="sending" @click="handoff">转人工</van-button><van-button plain size="small" @click="open = false">关闭</van-button></div></header>
        <nav class="service-quick-actions" aria-label="常见问题">
          <button v-for="question in quickQuestions" :key="question" type="button" class="quick-question" :disabled="sending" @click="askQuickQuestion(question)">{{ question }}</button>
        </nav>
        <div ref="messageList" class="service-messages" aria-live="polite">
          <p v-if="!messages.length" class="service-welcome">你好，我可以帮你了解商品、订单、物流、退款进度、优惠券和会员服务。</p>
          <div v-for="(item, index) in messages" :key="index" class="service-turn" :class="item.role"><p class="service-message">{{ item.content || '正在思考…' }}</p><small v-if="item.tool" class="service-sources">已执行：{{ toolLabel(item.tool) }}（只读）</small><small v-if="item.sources?.length" class="service-sources">依据：{{ item.sources.map(source => source.title).join('、') }}</small></div>
        </div>
        <form class="service-form" @submit.prevent="send"><van-field v-model="draft" aria-label="客服问题" placeholder="输入你的问题" maxlength="1000" :disabled="sending"/><van-button native-type="submit" type="primary" :loading="sending" :disabled="!draft.trim()">发送</van-button></form>
      </section>
    </van-popup>
  </div>
</template>
<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { aiApi } from '../api'
const router = useRouter(), open = ref(false), draft = ref(''), sending = ref(false), messages = ref([]), conversationId = ref(''), handoffTicket = ref(null), messageList = ref(null)
const quickQuestions = ['怎么查我的订单？', '退款规则是什么？', '物流在哪里看？', '优惠券怎么领？']
const toolLabel = tool => ({ query_order: '订单查询', query_logistics: '物流查询', query_refund: '退款查询', query_product: '商品查询', query_coupon: '优惠券查询' }[tool] || '业务查询')
const handoffStatus = computed(() => ({ 0: '待接管', 1: '处理中', 2: '已解决' }[handoffTicket.value?.status] || '已提交'))
function askQuickQuestion(question) { draft.value = question; send() }
async function handoff() {
  if (!localStorage.getItem('member-token')) { showToast('请登录后使用平台客服'); router.push('/account'); return }
  const latest = messages.value.filter(item => item.role === 'user').at(-1)?.content || '需要人工客服协助'
  try { handoffTicket.value = (await aiApi.handoff({ conversationId: conversationId.value || undefined, message: latest })).data; showToast('已提交平台人工客服工单') } catch (error) { showToast(error) }
}
async function loadHandoff() {
  if (!localStorage.getItem('member-token')) return
  try {
    const records = (await aiApi.mine({ page: 1, size: 10 })).data?.records || []
    handoffTicket.value = records.find(ticket => ticket.status !== 2) || records[0] || null
  } catch { /* 客服状态读取失败不影响 AI 对话 */ }
}
watch(open, value => { if (value) loadHandoff() })
async function scrollToBottom() { await nextTick(); messageList.value?.scrollTo({ top: messageList.value.scrollHeight, behavior: 'smooth' }) }
async function send() {
  const content = draft.value.trim()
  if (!localStorage.getItem('member-token')) { showToast('请登录后使用平台客服'); router.push('/account'); return }
  if (!content || sending.value) return
  messages.value.push({ role: 'user', content }, { role: 'assistant', content: '' }); draft.value = ''; sending.value = true; await scrollToBottom()
  const assistant = messages.value.at(-1)
  try {
    await aiApi.streamChat({ conversationId: conversationId.value || undefined, message: content }, event => {
      if (event.type === 'meta') conversationId.value = event.conversationId
      if (event.type === 'text') assistant.content += event.content
      if (event.type === 'error') assistant.content = event.message
      if (event.type === 'tool_call') assistant.tool = event.name
      if (event.type === 'sources') assistant.sources = event.items
      scrollToBottom()
    })
  } catch (error) { assistant.content = error.message || '客服服务暂不可用，请稍后重试' } finally { sending.value = false; scrollToBottom() }
}
</script>
<style scoped>
.service-trigger{position:fixed;right:20px;bottom:86px;z-index:20;width:52px;height:52px;background:var(--sea);border-color:var(--sea);box-shadow:0 8px 20px rgba(12,93,117,.28);font-weight:800}.service-panel{height:100%;display:flex;flex-direction:column;background:#f7faf9}.service-panel header{display:flex;align-items:center;justify-content:space-between;padding:18px 20px;background:#fff;border-bottom:1px solid var(--line)}.service-panel h2{margin:0;font-size:20px}.service-panel .eyebrow{margin-bottom:4px}.service-panel header small{display:block;color:#64748b;margin-top:4px}.service-header-actions{display:flex;gap:6px}.service-quick-actions{display:flex;gap:8px;overflow-x:auto;padding:12px 18px 0}.quick-question{flex:0 0 auto;min-height:44px;padding:0 12px;border:1px solid var(--line);border-radius:999px;background:#fff;color:var(--ink);font:inherit;white-space:nowrap}.quick-question:disabled{opacity:.5}.quick-question:not(:disabled):active{background:#e8f3f2}.service-messages{flex:1;overflow:auto;padding:18px}.service-welcome{margin:0;color:#64748b;line-height:1.6}.service-turn{max-width:84%;margin:0 0 12px}.service-turn.user{margin-left:auto}.service-message{width:max-content;max-width:100%;margin:0;padding:11px 13px;border-radius:14px;line-height:1.55;white-space:pre-wrap;overflow-wrap:anywhere}.service-turn.user .service-message{margin-left:auto;background:var(--sea);color:#fff;border-bottom-right-radius:4px}.service-turn.assistant .service-message{background:#fff;color:var(--ink);border:1px solid var(--line);border-bottom-left-radius:4px}.service-sources{display:block;margin:5px 4px 0;color:#64748b;font-size:11px}.service-form{display:flex;gap:8px;align-items:center;padding:12px;background:#fff;border-top:1px solid var(--line)}.service-form .van-cell{padding:8px 0}.service-form :deep(.van-field){flex:1;border:1px solid var(--line);border-radius:10px;padding:0 10px}.service-form .van-button{min-width:60px}@media(min-width:1025px){.service-trigger{right:32px;bottom:32px}.service-panel{max-width:480px;margin-left:auto}}
</style>
