<template>
  <div class="sub-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">SKU 管理</h2>
        <p class="page-desc">SPU #{{ spuId }} · 多币种定价与库存</p>
      </div>
      <div class="head-actions">
        <a-space>
          <a-button @click="$router.back()">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="margin-right:4px;vertical-align:middle"><polyline points="15 18 9 12 15 6"/></svg>
            返回
          </a-button>
          <a-button @click="openBatchPrice">批量调价</a-button>
          <a-button type="primary" @click="openAdd">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;vertical-align:middle"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            新增 SKU
          </a-button>
        </a-space>
      </div>
    </div>

    <a-card :bordered="false">
      <a-table :columns="columns" :data-source="list" :loading="loading"
        row-key="id" size="middle" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'price'">${{ record.price }}</template>
          <template v-if="column.key === 'stock'">
            <a-tag :color="record.stock > 10 ? 'green' : record.stock > 0 ? 'orange' : 'red'">
              {{ record.stock ?? '--' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button size="small" type="link" @click="openEdit(record)">编辑</a-button>
              <a-button size="small" type="link" @click="openStock(record)">库存</a-button>
              <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
                <a-button size="small" type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Add / Edit Modal -->
    <a-modal v-model:open="modalOpen" :title="editingId ? '编辑 SKU' : '新增 SKU'"
      @ok="handleSave" :confirm-loading="saving" destroy-on-close>
      <a-form :model="form" layout="vertical" style="margin-top:16px">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="SKU 编码" required>
              <a-input v-model:value="form.skuCode" placeholder="如 SPU001-BLK-M" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="币种" required>
              <a-select v-model:value="form.currency" :options="currencyOptions" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="价格" required>
              <a-input-number v-model:value="form.price" :min="0" :precision="2" style="width:100%" prefix="$" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="重量 (kg)">
              <a-input-number v-model:value="form.weight" :min="0" :precision="3" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="状态">
              <a-switch v-model:checked="form.status" :checked-value="1" :unchecked-value="0"
                checked-children="启用" un-checked-children="禁用" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="规格属性">
          <a-input v-model:value="form.specs" placeholder='如 {"颜色":"黑色","尺寸":"M"}' />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Stock Modal -->
    <a-modal v-model:open="stockModalOpen" title="修改库存" @ok="handleStockSave" :confirm-loading="stockSaving">
      <a-form layout="vertical" style="margin-top:16px">
        <a-form-item label="SKU">
          <a-input :value="stockTarget?.skuCode" disabled />
        </a-form-item>
        <a-form-item label="当前库存">
          <a-input-number :value="stockTarget?.stock" disabled style="width:100%" />
        </a-form-item>
        <a-form-item label="新库存值" required>
          <a-input-number v-model:value="newStock" :min="0" style="width:100%" placeholder="输入新的库存数量" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Batch Price Modal -->
    <a-modal v-model:open="batchPriceOpen" title="批量调价" @ok="handleBatchPrice" :confirm-loading="batchSaving">
      <a-form layout="vertical" style="margin-top:16px">
        <a-form-item label="调价方式" required>
          <a-radio-group v-model:value="priceMode">
            <a-radio value="fixed">固定金额调整</a-radio>
            <a-radio value="percent">百分比调整</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="priceMode === 'fixed' ? '调整金额 ($)' : '调整比例 (%)'" required>
          <a-input-number v-model:value="priceValue" style="width:100%"
            :placeholder="priceMode === 'fixed' ? '+5.00 或 -3.00' : '+10 或 -20'" />
        </a-form-item>
        <a-alert v-if="list.length" type="info" show-icon style="margin-top:4px">
          将对当前 {{ list.length }} 个 SKU {{ priceMode === 'fixed' ? '统一调整金额' : '按比例调整价格' }}
        </a-alert>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { getSkuList, createSku, updateSku, deleteSku, batchSkuPrice, updateSkuStock } from '@/api/product'

const route = useRoute()
const spuId = route.params.spuId

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const modalOpen = ref(false)
const editingId = ref(null)

const currencyOptions = [
  { label: 'USD ($)', value: 'USD' },
  { label: 'EUR (€)', value: 'EUR' },
  { label: 'GBP (£)', value: 'GBP' },
  { label: 'JPY (¥)', value: 'JPY' },
  { label: 'CNY (¥)', value: 'CNY' }
]

const form = reactive({
  skuCode: '',
  price: null,
  currency: 'USD',
  weight: null,
  status: 1,
  specs: ''
})

const stockModalOpen = ref(false)
const stockSaving = ref(false)
const stockTarget = ref(null)
const newStock = ref(0)

const batchPriceOpen = ref(false)
const batchSaving = ref(false)
const priceMode = ref('fixed')
const priceValue = ref(null)

const columns = [
  { title: 'SKU 编码', dataIndex: 'skuCode' },
  { title: '价格', key: 'price', width: 120 },
  { title: '币种', dataIndex: 'currency', width: 70 },
  { title: '库存', key: 'stock', width: 80 },
  { title: '重量(kg)', dataIndex: 'weight', width: 100 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 180 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getSkuList(spuId)
    list.value = Array.isArray(res.data) ? res.data : []
  } finally { loading.value = false }
}

function resetForm() {
  form.skuCode = ''
  form.price = null
  form.currency = 'USD'
  form.weight = null
  form.status = 1
  form.specs = ''
  editingId.value = null
}

function openAdd() {
  resetForm()
  modalOpen.value = true
}

function openEdit(r) {
  editingId.value = r.id
  form.skuCode = r.skuCode || ''
  form.price = r.price ?? null
  form.currency = r.currency || 'USD'
  form.weight = r.weight ?? null
  form.status = r.status ?? 1
  form.specs = r.specs || ''
  modalOpen.value = true
}

async function handleSave() {
  if (!form.skuCode || form.price == null) {
    message.warning('请填写 SKU 编码和价格')
    return
  }
  saving.value = true
  try {
    const payload = {
      spuId: Number(spuId),
      skuCode: form.skuCode,
      price: form.price,
      currency: form.currency,
      weight: form.weight,
      status: form.status,
      specs: form.specs || undefined
    }
    if (editingId.value) {
      await updateSku(editingId.value, payload)
      message.success('已更新')
    } else {
      await createSku(payload)
      message.success('已创建')
    }
    modalOpen.value = false
    fetchData()
  } catch { /* ignore */ } finally { saving.value = false }
}

function openStock(r) {
  stockTarget.value = r
  newStock.value = r.stock ?? 0
  stockModalOpen.value = true
}

async function handleStockSave() {
  stockSaving.value = true
  try {
    await updateSkuStock(stockTarget.value.id, newStock.value)
    message.success('库存已更新')
    stockModalOpen.value = false
    fetchData()
  } catch { /* ignore */ } finally { stockSaving.value = false }
}

function openBatchPrice() {
  priceMode.value = 'fixed'
  priceValue.value = null
  batchPriceOpen.value = true
}

async function handleBatchPrice() {
  if (priceValue.value == null) {
    message.warning('请输入调整值')
    return
  }
  batchSaving.value = true
  try {
    await batchSkuPrice({
      spuId: Number(spuId),
      adjustType: priceMode.value,
      adjustValue: priceValue.value
    })
    message.success('批量调价已生效')
    batchPriceOpen.value = false
    fetchData()
  } catch { /* ignore */ } finally { batchSaving.value = false }
}

async function handleDelete(id) {
  try {
    await deleteSku(id)
    message.success('已删除')
    fetchData()
  } catch { /* ignore */ }
}

onMounted(fetchData)
</script>
