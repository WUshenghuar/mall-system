<template>
  <div class="sub-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">活动管理</h2>
        <p class="page-desc">创建与管理营销活动</p>
      </div>
      <div class="head-actions">
        <a-button type="primary" @click="openAdd">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;vertical-align:middle"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新增活动
        </a-button>
      </div>
    </div>

    <a-card :bordered="false">
      <a-table :columns="columns" :data-source="list" :loading="loading"
        row-key="id" size="middle" :pagination="pagination" @change="onPage">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag v-if="record.status === 0" color="default">未开始</a-tag>
            <a-tag v-else-if="record.status === 1" color="green">进行中</a-tag>
            <a-tag v-else color="default">已结束</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button size="small" type="link" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
                <a-button size="small" type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Add / Edit Modal -->
    <a-modal v-model:open="modalOpen" :title="editingId ? '编辑活动' : '新增活动'"
      @ok="handleSave" :confirm-loading="saving" destroy-on-close>
      <a-form :model="form" layout="vertical" style="margin-top:16px">
        <a-form-item label="活动名称" required>
          <a-input v-model:value="form.activityName" placeholder="如 618年中大促" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="开始时间" required>
              <a-date-picker v-model:value="form.startTime" show-time style="width:100%"
                placeholder="选择开始时间" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结束时间" required>
              <a-date-picker v-model:value="form.endTime" show-time style="width:100%"
                placeholder="选择结束时间" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="活动描述">
          <a-textarea v-model:value="form.description" :rows="3" placeholder="活动说明（可选）" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getActivityPage, createActivity, updateActivity, deleteActivity } from '@/api/marketing'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const modalOpen = ref(false)
const editingId = ref(null)

const form = reactive({
  activityName: '',
  startTime: null,
  endTime: null,
  description: ''
})

const pagination = ref({ current: 1, pageSize: 10, total: 0 })

const columns = [
  { title: '活动名称', dataIndex: 'activityName' },
  { title: '开始时间', dataIndex: 'startTime' },
  { title: '结束时间', dataIndex: 'endTime' },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 150 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getActivityPage({ page: pagination.value.current, size: pagination.value.pageSize })
    list.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } finally { loading.value = false }
}

function onPage(p) { pagination.value.current = p.current; fetchData() }

function resetForm() {
  form.activityName = ''
  form.startTime = null
  form.endTime = null
  form.description = ''
  editingId.value = null
}

function openAdd() {
  resetForm()
  modalOpen.value = true
}

function openEdit(r) {
  editingId.value = r.id
  form.activityName = r.activityName || ''
  form.startTime = r.startTime || null
  form.endTime = r.endTime || null
  form.description = r.description || ''
  modalOpen.value = true
}

async function handleSave() {
  if (!form.activityName || !form.startTime || !form.endTime) {
    message.warning('请填写活动名称和时间')
    return
  }
  saving.value = true
  try {
    const payload = { ...form }
    if (editingId.value) {
      await updateActivity(editingId.value, payload)
      message.success('已更新')
    } else {
      await createActivity(payload)
      message.success('已创建')
    }
    modalOpen.value = false
    fetchData()
  } catch { /* ignore */ } finally { saving.value = false }
}

async function handleDelete(id) {
  try {
    await deleteActivity(id)
    message.success('已删除')
    fetchData()
  } catch { /* ignore */ }
}

onMounted(fetchData)
</script>
