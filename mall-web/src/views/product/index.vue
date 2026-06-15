<template>
  <div class="product-page">
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
          <template #title>
            <span>商品列表（SPU）</span>
          </template>
          <template #extra>
            <a-space>
              <a-input-search
                v-model:value="keyword"
                placeholder="搜索商品名称"
                style="width: 220px"
                @search="fetchData"
                allow-clear
              />
              <a-button type="primary" @click="showAddModal = true">
                <plus-outlined /> 添加商品
              </a-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { getCategoryTree, getSpuPage, deleteSpu, publishSpu } from '@/api/product'

const loading = ref(false)
const keyword = ref('')
const categoryId = ref(null)
const categoryTree = ref([])
const dataSource = ref([])
const showAddModal = ref(false)

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
  message.info(`查看 ${record.spuName} 详情（SKU 列表开发中）`)
}

onMounted(() => {
  fetchCategories()
  fetchData()
})
</script>

<style scoped>
.product-page {
  max-width: 1400px;
}
</style>
