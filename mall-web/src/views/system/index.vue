<template>
  <div class="system-page">
    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="users" tab="用户管理">
        <a-card :bordered="false" size="small">
          <a-table
            :columns="userColumns"
            :data-source="users"
            :pagination="false"
            row-key="id"
            size="small"
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
          <a-row :gutter="16">
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
        </a-card>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const activeTab = ref('users')
const users = ref([])
const roles = ref([])

const userColumns = [
  { title: '用户名', dataIndex: 'username', key: 'username', width: 100 },
  { title: '姓名', dataIndex: 'realName', key: 'realName', width: 100 },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '手机', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '状态', key: 'status', width: 70 },
  { title: '角色', key: 'roles', width: 200 }
]

const roleMap = { store_manager: '店长', ops_specialist: '运营', cs_specialist: '客服', finance: '财务' }

async function fetchUsers() {
  try {
    const res = await request.get('/system/user/list')
    users.value = (res.data || []).map(u => ({
      ...u,
      roles: (u.roleKeys || []).map(k => roleMap[k] || k)
    }))
  } catch { /* ignore */ }
}

async function fetchRoles() {
  try {
    const res = await request.get('/system/role/list')
    roles.value = res.data || []
  } catch { /* ignore */ }
}

onMounted(() => {
  fetchUsers()
  fetchRoles()
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
</style>
