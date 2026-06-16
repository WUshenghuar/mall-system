<template>
  <div class="system-page">
    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <a-tab-pane key="users" tab="用户管理">
        <a-card :bordered="false" size="small">
          <div v-if="usersError" class="error-state">
            <a-result status="error" title="加载失败" :sub-title="usersError">
              <template #extra>
                <a-button type="primary" @click="fetchUsers">重试</a-button>
              </template>
            </a-result>
          </div>
          <a-table
            v-else
            :columns="userColumns"
            :data-source="users"
            :loading="usersLoading"
            :pagination="userPagination"
            row-key="id"
            size="small"
            @change="onUserTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 1 ? 'green' : 'red'">
                  {{ record.status === 1 ? '正常' : '停用' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'roles'">
                <a-tag v-for="r in record.roles" :key="r" size="small">{{ r }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="roles" tab="角色管理">
        <a-card :bordered="false" size="small">
          <div v-if="rolesError" class="error-state">
            <a-result status="error" title="加载失败" :sub-title="rolesError">
              <template #extra>
                <a-button type="primary" @click="fetchRoles">重试</a-button>
              </template>
            </a-result>
          </div>
          <a-spin v-else :spinning="rolesLoading">
            <a-empty v-if="!rolesLoading && roles.length === 0" description="暂无角色" />
            <a-row v-else :gutter="16">
              <a-col :span="8" v-for="r in roles" :key="r.id">
                <a-card size="small" hoverable class="role-card">
                  <template #title>
                    <span>{{ r.roleName }}</span>
                    <a-tag :color="r.status === 1 ? 'green' : 'default'" style="margin-left:8px">
                      {{ r.status === 1 ? '启用' : '停用' }}
                    </a-tag>
                  </template>
                  <p class="role-key">{{ r.roleKey }}</p>
                  <p class="role-remark">{{ r.remark }}</p>
                </a-card>
              </a-col>
            </a-row>
          </a-spin>
        </a-card>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '@/utils/request'

const activeTab = ref('users')
const users = ref([])
const roles = ref([])
const usersLoading = ref(false)
const usersError = ref('')
const rolesLoading = ref(false)
const rolesError = ref('')
const rolesLoaded = ref(false)

const userPagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: total => `共 ${total} 条`
})

const userColumns = [
  { title: '用户名', dataIndex: 'username', key: 'username', width: 100 },
  { title: '姓名', dataIndex: 'realName', key: 'realName', width: 100 },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '手机', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '状态', key: 'status', width: 70 },
  { title: '角色', key: 'roles', width: 200 }
]

// 从角色数据动态构建中文名映射，不再硬编码
const roleNameMap = computed(() => {
  const map = {}
  roles.value.forEach(r => {
    map[r.roleKey] = r.roleName
  })
  return map
})

async function fetchUsers() {
  usersLoading.value = true
  usersError.value = ''
  try {
    const res = await request.get('/system/user/list', {
      params: {
        page: userPagination.value.current,
        size: userPagination.value.pageSize
      }
    })
    const pageData = res.data
    users.value = (pageData.records || []).map(u => ({
      ...u,
      roles: (u.roleKeys || []).map(k => roleNameMap.value[k] || k)
    }))
    userPagination.value.total = pageData.total || 0
  } catch (e) {
    usersError.value = e.message || '加载用户列表失败，请检查网络连接'
    users.value = []
  } finally {
    usersLoading.value = false
  }
}

async function fetchRoles() {
  rolesLoading.value = true
  rolesError.value = ''
  try {
    const res = await request.get('/system/role/list')
    roles.value = res.data || []
    rolesLoaded.value = true
  } catch (e) {
    rolesError.value = e.message || '加载角色列表失败，请检查网络连接'
    roles.value = []
  } finally {
    rolesLoading.value = false
  }
}

function onTabChange(key) {
  // 懒加载：首次切换到角色 tab 时才请求角色数据
  if (key === 'roles' && !rolesLoaded.value) {
    fetchRoles()
  }
}

function onUserTableChange(pagination) {
  userPagination.value.current = pagination.current
  userPagination.value.pageSize = pagination.pageSize
  fetchUsers()
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.system-page { max-width: 1000px; }

.role-card {
  margin-bottom: var(--space-4);
}
.role-card .role-key {
  font-family: monospace;
  font-size: var(--text-xs);
  color: var(--color-slate);
  margin: 0;
}
.role-card .role-remark {
  font-size: var(--text-sm);
  color: var(--color-slate-light);
  margin: var(--space-2) 0 0;
}

.error-state {
  padding: var(--space-8) 0;
}
</style>
