<template>
  <div class="customer-service">
    <van-button round type="primary" class="service-trigger" aria-label="打开平台客服" @click="open = true">客服</van-button>
    <van-popup v-model:show="open" position="bottom" round :style="{ height: '72%' }">
      <section class="service-panel" aria-label="平台智能客服">
        <header><div><p class="eyebrow">PLATFORM SUPPORT</p><h2>海路客服</h2><small v-if="handoffTicket">工单 {{ handoffTicket.ticketNo }} · {{ handoffStatus }}</small><small v-if="handoffTicket?.agentReply" class="service-agent-reply" role="status" aria-atomic="true">客服回复：{{ handoffTicket.agentReply }}</small><small v-if="handoffTicket?.handledNote" class="service-resolution" role="status" aria-atomic="true">处理结果：{{ handoffTicket.handledNote }}</small></div><div class="service-header-actions"><van-button plain size="small" type="warning" :disabled="sending" @click="handoff">转人工</van-button><van-button v-if="handoffTicket?.status === 1" plain size="small" type="primary" :disabled="sending" @click="humanMode = true">补充留言</van-button><van-button plain size="small" @click="open = false">关闭</van-button></div></header>
        <nav class="service-quick-actions" aria-label="常见问题">
          <button v-for="question in quickQuestions" :key="question" type="button" class="quick-question" :disabled="sending" @click="askQuickQuestion(question)">{{ question }}</button>
        </nav>
        <div ref="messageList" class="service-messages" aria-live="polite">
          <p v-if="!messages.length" class="service-welcome">你好，我可以帮你了解商品、订单、物流、退款进度、优惠券和会员服务。</p>
          <div v-for="(item, index) in messages" :key="index" class="service-turn" :class="item.role"><small v-if="item.role === 'agent'" class="service-agent-label">平台客服</small><p class="service-message">{{ item.content || '正在思考…' }}</p><small v-if="item.tool" class="service-sources">已执行：{{ toolLabel(item.tool) }}（只读）</small><small v-if="item.sources?.length" class="service-sources">依据：{{ item.sources.map(source => source.title).join('、') }}</small><div v-if="item.role === 'assistant' && item.id" class="service-feedback" aria-label="回答评价"><button type="button" class="message-feedback" :disabled="item.feedback === 1 || item.feedback === -1" @click="rate(item, 1)">{{ item.feedback === 1 ? '已反馈' : '有帮助' }}</button><button type="button" class="message-feedback" :disabled="item.feedback === 1 || item.feedback === -1" @click="rate(item, -1)">{{ item.feedback === -1 ? '已反馈' : '没帮助' }}</button></div></div>
        </div>
        <form class="service-form" @submit.prevent="send"><van-field v-model="draft" aria-label="客服问题" :placeholder="humanMode ? '给平台客服留言' : '输入你的问题'" maxlength="1000" :disabled="sending"/><van-button native-type="submit" type="primary" :loading="sending" :disabled="!draft.trim()">{{ humanMode ? '留言' : '发送' }}</van-button></form>
      </section>
    </van-popup>
  </div>
</template>
<script setup>
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { aiApi } from '../api'
const router = useRouter(), open = ref(false), draft = ref(''), sending = ref(false), humanMode = ref(false), messages = ref([]), conversationId = ref(''), handoffTicket = ref(null), messageList = ref(null)
let handoffTimer
const quickQuestions = ['怎么查我的订单？', '退款规则是什么？', '物流在哪里看？', '优惠券怎么领？']
const toolLabel = tool => ({ query_order: '订单查询', query_logistics: '物流查询', query_refund: '退款查询', query_product: '商品查询', query_coupon: '优惠券查询', query_member: '会员查询', query_tax: '税费查询' }[tool] || '业务查询')
const handoffStatus = computed(() => ({ 0: '待接管', 1: '处理中', 2: '已解决' }[handoffTicket.value?.status] || '已提交'))
function askQuickQuestion(question) { draft.value = question; send() }
async function handoff() {
  if (!localStorage.getItem('member-token')) { showToast('请登录后使用平台客服'); router.push('/account'); return }
  const latest = messages.value.filter(item => item.role === 'user').at(-1)?.content || '需要人工客服协助'
  try { handoffTicket.value = (await aiApi.handoff({ conversationId: conversationId.value || undefined, message: latest })).data; showToast('已提交平台人工客服工单') } catch (error) { showToast(error) }
}
async function loadRecent(force = false) {
  if (!localStorage.getItem('member-token') || (!force && messages.value.length)) return
  try {
    const records = (await aiApi.recent()).data || []
    if (!records.length || (sending.value && force)) return
    conversationId.value = records[0].sessionId || conversationId.value
    messages.value = records.filter(item => item.content).map(item => ({ id: item.id, role: item.role, content: item.content, feedback: item.feedback }))
    scrollToBottom()
  } catch { /* 历史加载失败不影响新对话 */ }
}
async function loadHandoff() {
  if (!localStorage.getItem('member-token')) return
  try {
    const records = (await aiApi.mine({ page: 1, size: 10 })).data?.records || []
    const sessionId = conversationId.value || handoffTicket.value?.conversationId
    const candidates = sessionId ? records.filter(ticket => ticket.conversationId === sessionId) : records
    const nextTicket = candidates.find(ticket => ticket.status !== 2) || candidates[0] || null
    if (nextTicket && (!handoffTicket.value || handoffTicket.value.id === nextTicket.id || handoffTicket.value.status === 2)) handoffTicket.value = nextTicket
  } catch { /* 客服状态读取失败不影响 AI 对话 */ }
}
watch(open, value => { if (value) { loadRecent(true).finally(loadHandoff); handoffTimer = window.setInterval(() => { loadHandoff(); if (!sending.value) loadRecent(true) }, 15000) } else { window.clearInterval(handoffTimer) } })
onUnmounted(() => window.clearInterval(handoffTimer))
async function scrollToBottom() { await nextTick(); messageList.value?.scrollTo({ top: messageList.value.scrollHeight, behavior: 'smooth' }) }
async function send() {
  const content = draft.value.trim()
  if (!localStorage.getItem('member-token')) { showToast('请登录后使用平台客服'); router.push('/account'); return }
  if (!content || sending.value) return
  if (humanMode.value) { await sendMemberMessage(content); return }
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
    try {
      const records = (await aiApi.recent()).data || []
      const latestReply = records.slice().reverse().find(item => item.role === 'assistant' && item.content)
      if (latestReply) { assistant.id = latestReply.id; assistant.feedback = latestReply.feedback }
    } catch { /* 反馈同步失败不影响已经完成的客服回答 */ }
  } catch (error) { assistant.content = error.message || '客服服务暂不可用，请稍后重试' } finally { sending.value = false; scrollToBottom() }
}
async function sendMemberMessage(content) { if (!handoffTicket.value || handoffTicket.value.status !== 1) { humanMode.value = false; showToast('请先提交人工客服工单'); return }; messages.value.push({ role: 'user', content }); draft.value = ''; sending.value = true; try { await aiApi.memberMessage(handoffTicket.value.id, { message: content }); humanMode.value = false; showToast('留言已发送给平台客服'); await scrollToBottom() } catch (error) { messages.value.pop(); showToast(error) } finally { sending.value = false } }
async function rate(item, value) { if (!item.id || item.feedback === 1 || item.feedback === -1) return; try { await aiApi.feedback({ messageId: item.id, feedback: value }); item.feedback = value; showToast('感谢你的反馈') } catch (error) { showToast(error) } }
</script>
<style scoped>
.service-trigger{position:fixed;right:20px;bottom:86px;z-index:20;width:52px;height:52px;background:var(--sea);border-color:var(--sea);box-shadow:0 8px 20px rgba(12,93,117,.28);font-weight:800}.service-panel{height:100%;display:flex;flex-direction:column;background:#f7faf9}.service-panel header{display:flex;align-items:center;justify-content:space-between;padding:18px 20px;background:#fff;border-bottom:1px solid var(--line)}.service-panel h2{margin:0;font-size:20px}.service-panel .eyebrow{margin-bottom:4px}.service-panel header small{display:block;color:#64748b;margin-top:4px}.service-header-actions{display:flex;gap:6px}.service-quick-actions{display:flex;gap:8px;overflow-x:auto;padding:12px 18px 0}.quick-question{flex:0 0 auto;min-height:44px;padding:0 12px;border:1px solid var(--line);border-radius:999px;background:#fff;color:var(--ink);font:inherit;white-space:nowrap}.quick-question:disabled{opacity:.5}.quick-question:not(:disabled):active{background:#e8f3f2}.service-messages{flex:1;overflow:auto;padding:18px}.service-welcome{margin:0;color:#64748b;line-height:1.6}.service-turn{max-width:84%;margin:0 0 12px}.service-turn.user{margin-left:auto}.service-message{width:max-content;max-width:100%;margin:0;padding:11px 13px;border-radius:14px;line-height:1.55;white-space:pre-wrap;overflow-wrap:anywhere}.service-turn.user .service-message{margin-left:auto;background:var(--sea);color:#fff;border-bottom-right-radius:4px}.service-turn.assistant .service-message{background:#fff;color:var(--ink);border:1px solid var(--line);border-bottom-left-radius:4px}.service-sources{display:block;margin:5px 4px 0;color:#64748b;font-size:11px}.service-form{display:flex;gap:8px;align-items:center;padding:12px;background:#fff;border-top:1px solid var(--line)}.service-form .van-cell{padding:8px 0}.service-form :deep(.van-field){flex:1;border:1px solid var(--line);border-radius:10px;padding:0 10px}.service-form .van-button{min-width:60px}@media(min-width:1025px){.service-trigger{right:32px;bottom:32px}.service-panel{max-width:480px;margin-left:auto}}
.service-turn.agent .service-message{background:#fff8f1;color:var(--ink);border:1px solid #f0d8b9;border-bottom-left-radius:4px}.service-agent-label{display:block;margin:0 4px 3px;color:var(--copper);font-size:11px}.service-agent-reply{color:var(--copper)!important}.service-resolution{color:var(--sea)!important}.service-feedback{display:flex;gap:4px;margin-top:2px}.message-feedback{min-height:44px;padding:0 8px;border:0;background:transparent;color:#64748b;font:inherit;font-size:12px}.message-feedback:not(:disabled):active,.message-feedback:focus-visible{color:var(--sea)}.message-feedback:disabled{color:var(--sea);opacity:.8}
</style>
