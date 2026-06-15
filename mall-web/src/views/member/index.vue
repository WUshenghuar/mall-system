<template>
  <div class="member-page">
    <a-card :bordered="false">
      <template #extra>
        <a-input-search
          v-model:value="keyword"
          placeholder="搜索昵称或邮箱"
          style="width: 240px"
          @search="fetchData"
          allow-clear
        />
      </template>

      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        @change="onTableChange"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'level'">
            <a-tag v-if="record.level === 0">普通</a-tag>
            <a-tag v-else-if="record.level === 1" color="gold">Gold</a-tag>
            <a-tag v-else color="purple">Platinum</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="showAdjust(record)">
              调整积分
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Points adjust modal -->
    <a-modal v-model:open="modalOpen" title="调整积分" @ok="handleAdjust">
      <a-form layout="vertical">
        <a-form-item label="会员">
          <span>{{ currentMember?.nickName }} ({{ currentMember?.email }})</span>
        </a-form-item>
        <a-form-item label="当前积分">
          <span>{{ currentMember?.points }}</span>
        </a-form-item>
        <a-form-item label="变更积分（正数增加，负数减少）">
          <a-input-number v-model:value="pointsChange" style="width: 200px" />
        </a-form-item>
        <a-form-item label="原因">
          <a-input v-model:value="pointsReason" placeholder="变更原因" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getMemberPage, adjustPoints } from '@/api/member'

const loading = ref(false)
const keyword = ref('')
const dataSource = ref([])
const modalOpen = ref(false)
const currentMember = ref(null)
const pointsChange = ref(0)
const pointsReason = ref('')

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: t => `共 ${t} 条`
})

const columns = [
  { title: '昵称', dataIndex: 'nickName', key: 'nickName', width: 120 },
  { title: '邮箱', dataIndex: 'email', key: 'email', ellipsis: true },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 140 },
  { title: '会员等级', key: 'level', width: 100 },
  { title: '积分', dataIndex: 'points', key: 'points', width: 80 },
  { title: '累计消费(USD)', dataIndex: 'totalAmount', key: 'totalAmount', width: 130 },
  { title: '操作', key: 'action', width: 100 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getMemberPage({
      page: pagination.current,
      size: pagination.pageSize,
      keyword: keyword.value
    })
    const page = res.data
    dataSource.value = page.records || []
    pagination.total = page.total || 0
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

function onTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

function showAdjust(record) {
  currentMember.value = record
  pointsChange.value = 0
  pointsReason.value = ''
  modalOpen.value = true
}

async function handleAdjust() {
  try {
    await adjustPoints(currentMember.value.id, pointsChange.value, pointsReason.value)
    message.success('积分已调整')
    modalOpen.value = false
    fetchData()
  } catch {
    // ignore
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.member-page {
  max-width: 1200px;
}
</style>
