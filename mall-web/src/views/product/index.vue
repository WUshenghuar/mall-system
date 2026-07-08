<template>
  <div class="sub-page">
    <!-- Page head -->
    <div class="page-head">
      <div>
        <h2 class="page-title">商品管理</h2>
        <p class="page-desc">管理 SPU、SKU 与多币种定价</p>
      </div>
      <div class="head-actions">
        <a-button type="primary" @click="showAddModal = true">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;vertical-align:middle"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          添加商品
        </a-button>
      </div>
    </div>

    <a-row :gutter="24">
      <!-- Category tree -->
      <a-col :span="5">
        <a-card title="商品分类" :bordered="false" size="small">
          <a-tree
            :tree-data="categoryTree"
            :field-names="{ title: 'categoryName', key: 'id', children: 'children' }"
            @select="onCategorySelect"
            block-node
          />
        </a-card>
      </a-col>

      <!-- SPU Table -->
      <a-col :span="19">
        <a-card :bordered="false">
          <template #extra>
            <a-space>
              <a-input-search
                v-model:value="keyword"
                placeholder="搜索商品名称"
                style="width: 220px"
                @search="fetchData"
                allow-clear
              />
            </a-space>
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
              <template v-if="column.key === 'status'">
                <a-tag v-if="record.status === 0" color="default">草稿</a-tag>
                <a-tag v-else-if="record.status === 1" color="green">已上架</a-tag>
                <a-tag v-else color="orange">已下架</a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-space size="small">
                  <a-button type="link" size="small" @click="viewDetail(record)">详情</a-button>
                  <a-button
                    v-if="record.status !== 1"
                    type="link"
                    size="small"
                    @click="togglePublish(record, 1)"
                  >上架</a-button>
                  <a-button
                    v-else
                    type="link"
                    size="small"
                    danger
                    @click="togglePublish(record, 2)"
                  >下架</a-button>
                  <a-popconfirm title="确认删除？" @confirm="handleDelete(record.id)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <!-- Add SPU Modal -->
    <a-modal
      v-model:open="showAddModal"
      title="添加商品"
      :confirm-loading="saving"
      @ok="handleAdd"
      @cancel="resetForm"
      destroy-on-close
    >
      <a-form :model="form" layout="vertical" style="margin-top: 16px">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="商品编码" required>
              <a-input v-model:value="form.spuCode" placeholder="如 SPU20240001" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="商品名称" required>
              <a-input v-model:value="form.spuName" placeholder="商品名称" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="分类" required>
              <a-tree-select
                v-model:value="form.categoryId"
                :tree-data="categoryTree"
                :field-names="{ label: 'categoryName', value: 'id', children: 'children' }"
                placeholder="选择分类"
                tree-default-expand-all
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="品牌">
              <a-select
                v-model:value="form.brandId"
                :options="brandOptions"
                placeholder="选择品牌"
                allow-clear
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="HS Code">
              <a-input v-model:value="form.customsCode" placeholder="如 6109.10.00" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="原产国">
              <a-input v-model:value="form.originCountry" placeholder="如 中国" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="商品描述">
          <a-textarea v-model:value="form.description" :rows="3" placeholder="商品描述（支持多语言 JSON）" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getCategoryTree, getSpuPage, deleteSpu, publishSpu, createSpu, getBrandList } from '@/api/product'

const router = useRouter()

const loading = ref(false)
const keyword = ref('')
const categoryId = ref(null)
const categoryTree = ref([])
const dataSource = ref([])
const showAddModal = ref(false)
const saving = ref(false)
const brandOptions = ref([])

const form = reactive({
  spuCode: '',
  spuName: '',
  categoryId: null,
  brandId: null,
  customsCode: '',
  originCountry: '',
  description: ''
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: t => `共 ${t} 条`
})

const columns = [
  { title: '商品编码', dataIndex: 'spuCode', key: 'spuCode', width: 120 },
  { title: '商品名称', dataIndex: 'spuName', key: 'spuName', ellipsis: true },
  { title: 'HS Code', dataIndex: 'customsCode', key: 'customsCode', width: 100 },
  { title: '原产国', dataIndex: 'originCountry', key: 'originCountry', width: 80 },
  { title: '销量', dataIndex: 'salesCount', key: 'salesCount', width: 70 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 180 }
]

function buildTree(list, parentId = 0) {
  return list
    .filter(item => item.parentId === parentId)
    .map(item => ({
      ...item,
      children: buildTree(list, item.id)
    }))
}

async function fetchCategories() {
  try {
    const res = await getCategoryTree()
    categoryTree.value = buildTree(res.data || [])
  } catch {
    // ignore
  }
}

function onCategorySelect(keys) {
  categoryId.value = keys[0] || null
  pagination.current = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getSpuPage({
      page: pagination.current,
      size: pagination.pageSize,
      categoryId: categoryId.value,
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

async function togglePublish(record, status) {
  try {
    await publishSpu(record.id, status)
    message.success(status === 1 ? '已上架' : '已下架')
    fetchData()
  } catch {
    // ignore
  }
}

async function handleDelete(id) {
  try {
    await deleteSpu(id)
    message.success('已删除')
    fetchData()
  } catch {
    // ignore
  }
}

function viewDetail(record) {
  router.push(`/product/sku/${record.id}`)
}

async function handleAdd() {
  if (!form.spuCode || !form.spuName || !form.categoryId) {
    message.warning('请填写商品编码、名称和分类')
    return
  }
  saving.value = true
  try {
    const payload = {
      spuCode: form.spuCode.trim(),
      spuName: form.spuName.trim(),
      categoryId: form.categoryId
    }
    if (form.brandId) payload.brandId = form.brandId
    if (form.customsCode) payload.customsCode = form.customsCode.trim()
    if (form.originCountry) payload.originCountry = form.originCountry.trim()
    // description 列是 MySQL JSON 类型，普通文本需包装为 JSON
    if (form.description) {
      const d = form.description.trim()
      try { JSON.parse(d); payload.description = d } catch { payload.description = JSON.stringify({ zh: d }) }
    }
    await createSpu(payload)
    message.success('添加成功')
    showAddModal.value = false
    resetForm()
    fetchData()
  } catch (e) {
    console.error('添加商品失败', e)
  } finally {
    saving.value = false
  }
}

function resetForm() {
  form.spuCode = ''
  form.spuName = ''
  form.categoryId = null
  form.brandId = null
  form.customsCode = ''
  form.originCountry = ''
  form.description = ''
}

async function fetchBrands() {
  try {
    const res = await getBrandList()
    const list = res.data?.records || res.data || []
    brandOptions.value = list.map(b => ({ label: b.brandName || b.name, value: b.id }))
  } catch {
    // ignore
  }
}

onMounted(() => {
  fetchCategories()
  fetchBrands()
  fetchData()
})
</script>

<style scoped>
/* Module accent — product = blue */
.product-page { max-width: 1400px; }
</style>
