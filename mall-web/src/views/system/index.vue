<template>
  <div class="sub-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">系统设置</h2>
        <p class="page-desc">用户与角色权限管理</p>
      </div>
    </div>

    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <!-- ═══ 用户管理 ═══ -->
      <a-tab-pane key="users" tab="用户管理">
        <a-card :bordered="false" size="small">
          <template #extra>
            <a-button type="primary" size="small" @click="openUserAdd">新增用户</a-button>
          </template>
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
              <template v-if="column.key === 'action'">
                <a-space size="small">
                  <a-button type="link" size="small" @click="openUserEdit(record)">编辑</a-button>
                  <a-popconfirm title="确定删除？" @confirm="handleUserDelete(record.id)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <!-- ═══ 角色管理 ═══ -->
      <a-tab-pane key="roles" tab="角色管理">
        <a-card :bordered="false" size="small">
          <template #extra>
            <a-button type="primary" size="small" @click="openRoleAdd">新增角色</a-button>
          </template>
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
                  <template #extra>
                    <a-space size="small">
                      <a-button type="link" size="small" @click="openRoleEdit(r)">编辑</a-button>
                      <a-popconfirm title="确定删除？" @confirm="handleRoleDelete(r.id)">
                        <a-button type="link" size="small" danger>删除</a-button>
                      </a-popconfirm>
                    </a-space>
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

    <!-- User Modal -->
    <a-modal v-model:open="userModalOpen" :title="userEditingId ? '编辑用户' : '新增用户'"
      @ok="handleUserSave" :confirm-loading="userSaving" destroy-on-close>
      <a-form :model="userForm" layout="vertical" style="margin-top:16px">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="用户名" required>
              <a-input v-model:value="userForm.username" :disabled="!!userEditingId" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="姓名" required>
              <a-input v-model:value="userForm.realName" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="邮箱">
              <a-input v-model:value="userForm.email" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="手机">
              <a-input v-model:value="userForm.phone" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item v-if="!userEditingId" label="密码" required>
          <a-input-password v-model:value="userForm.password" placeholder="至少 6 位" />
        </a-form-item>
        <a-form-item label="状态">
          <a-switch v-model:checked="userForm.status" :checked-value="1" :unchecked-value="0"
            checked-children="正常" un-checked-children="停用" />
        </a-form-item>
        <a-form-item label="角色">
          <a-checkbox-group v-model:value="userForm.roleKeys" :options="roleCheckboxOptions" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Role Modal -->
    <a-modal v-model:open="roleModalOpen" :title="roleEditingId ? '编辑角色' : '新增角色'"
      @ok="handleRoleSave" :confirm-loading="roleSaving" destroy-on-close>
      <a-form :model="roleForm" layout="vertical" style="margin-top:16px">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="角色名称" required>
              <a-input v-model:value="roleForm.roleName" placeholder="如 运营主管" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="角色标识" required>
              <a-input v-model:value="roleForm.roleKey" :disabled="!!roleEditingId" placeholder="如 ROLE_OPS" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="排序">
          <a-input-number v-model:value="roleForm.sort" :min="0" style="width:120px" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="roleForm.remark" :rows="2" />
        </a-form-item>
        <a-form-item label="状态">
          <a-switch v-model:checked="roleForm.status" :checked-value="1" :unchecked-value="0"
            checked-children="启用" un-checked-children="停用" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
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
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showTotal: total => `共 ${total} 条`
})

const userColumns = [
  { title: '用户名', dataIndex: 'username', width: 100 },
  { title: '姓名', dataIndex: 'realName', width: 100 },
  { title: '邮箱', dataIndex: 'email' },
  { title: '手机', dataIndex: 'phone', width: 130 },
  { title: '状态', key: 'status', width: 70 },
  { title: '角色', key: 'roles', width: 200 },
  { title: '操作', key: 'action', width: 130 }
]

// ── User CRUD state ──
const userModalOpen = ref(false)
const userSaving = ref(false)
const userEditingId = ref(null)
const userForm = ref({ username: '', realName: '', email: '', phone: '', password: '', status: 1, roleKeys: [] })

// ── Role CRUD state ──
const roleModalOpen = ref(false)
const roleSaving = ref(false)
const roleEditingId = ref(null)
const roleForm = ref({ roleName: '', roleKey: '', sort: 0, remark: '', status: 1 })

const roleNameMap = computed(() => {
  const map = {}
  roles.value.forEach(r => { map[r.roleKey] = r.roleName })
  return map
})

const roleCheckboxOptions = computed(() =>
  roles.value.map(r => ({ label: r.roleName, value: r.roleKey }))
)

// ── User API helpers ──
async function fetchUsers() {
  usersLoading.value = true; usersError.value = ''
  try {
    const res = await request.get('/system/user/list', {
      params: { page: userPagination.value.current, size: userPagination.value.pageSize }
    })
    const pageData = res.data
    users.value = (pageData.records || []).map(u => ({
      ...u,
      roles: (u.roleKeys || []).map(k => roleNameMap.value[k] || k)
    }))
    userPagination.value.total = pageData.total || 0
  } catch (e) { usersError.value = e.message || '加载失败'; users.value = [] }
  finally { usersLoading.value = false }
}

function onUserTableChange(pag) { userPagination.value.current = pag.current; userPagination.value.pageSize = pag.pageSize; fetchUsers() }

function openUserAdd() {
  userEditingId.value = null
  userForm.value = { username: '', realName: '', email: '', phone: '', password: '', status: 1, roleKeys: [] }
  userModalOpen.value = true
}

function openUserEdit(r) {
  userEditingId.value = r.id
  userForm.value = {
    username: r.username || '',
    realName: r.realName || '',
    email: r.email || '',
    phone: r.phone || '',
    password: '',
    status: r.status ?? 1,
    roleKeys: r.roleKeys || []
  }
  userModalOpen.value = true
}

async function handleUserSave() {
  const f = userForm.value
  if (!f.username || !f.realName) { message.warning('请填写用户名和姓名'); return }
  if (!userEditingId.value && !f.password) { message.warning('请填写密码'); return }
  userSaving.value = true
  try {
    const payload = { ...f }
    if (userEditingId.value) {
      if (!payload.password) delete payload.password
      await request.put(`/system/user/${userEditingId.value}`, payload)
      message.success('已更新')
    } else {
      await request.post('/system/user', payload)
      message.success('已创建')
    }
    userModalOpen.value = false
    fetchUsers()
  } catch { /* ignore */ } finally { userSaving.value = false }
}

async function handleUserDelete(id) {
  await request.delete(`/system/user/${id}`)
  message.success('已删除')
  fetchUsers()
}

// ── Role API helpers ──
async function fetchRoles() {
  rolesLoading.value = true; rolesError.value = ''
  try {
    const res = await request.get('/system/role/list')
    roles.value = res.data || []
    rolesLoaded.value = true
  } catch (e) { rolesError.value = e.message || '加载失败'; roles.value = [] }
  finally { rolesLoading.value = false }
}

function onTabChange(key) {
  if (key === 'roles' && !rolesLoaded.value) fetchRoles()
}

function openRoleAdd() {
  roleEditingId.value = null
  roleForm.value = { roleName: '', roleKey: '', sort: 0, remark: '', status: 1 }
  roleModalOpen.value = true
}

function openRoleEdit(r) {
  roleEditingId.value = r.id
  roleForm.value = {
    roleName: r.roleName || '',
    roleKey: r.roleKey || '',
    sort: r.sort ?? 0,
    remark: r.remark || '',
    status: r.status ?? 1
  }
  roleModalOpen.value = true
}

async function handleRoleSave() {
  const f = roleForm.value
  if (!f.roleName || !f.roleKey) { message.warning('请填写角色名称和标识'); return }
  roleSaving.value = true
  try {
    if (roleEditingId.value) {
      await request.put(`/system/role/${roleEditingId.value}`, f)
      message.success('已更新')
    } else {
      await request.post('/system/role', f)
      message.success('已创建')
    }
    roleModalOpen.value = false
    fetchRoles()
  } catch { /* ignore */ } finally { roleSaving.value = false }
}

async function handleRoleDelete(id) {
  await request.delete(`/system/role/${id}`)
  message.success('已删除')
  fetchRoles()
}

onMounted(() => { fetchUsers() })
</script>

<style scoped>
.system-page { max-width: 1000px; }
.role-card { margin-bottom: var(--space-4); }
.role-card .role-key { font-family: var(--font-mono); font-size: var(--text-xs); color: var(--color-slate); margin: 0; }
.role-card .role-remark { font-size: var(--text-sm); color: var(--color-slate-light); margin: var(--space-2) 0 0; }
</style>
