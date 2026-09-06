<template>
  <section class="account-page">
    <template v-if="!logged">
      <div class="auth-intro"><p class="eyebrow">MEMBER ACCESS</p><h1>登录后，继续你的跨境采购</h1><p>查看订单、管理收货地址和收藏商品。</p></div>
      <section class="auth-card" aria-label="会员登录与注册">
        <van-tabs v-model:active="authMode" class="auth-tabs">
          <van-tab title="登录" name="login"><div class="auth-form"><van-field v-model="phone" label="手机号" placeholder="请输入 11 位手机号" type="tel" maxlength="11" autocomplete="tel"/><van-field v-model="password" label="密码" placeholder="请输入密码" type="password" autocomplete="current-password"/><van-button block type="primary" class="auth-submit" :loading="submitting" @click="login">登录并继续</van-button></div></van-tab>
          <van-tab title="注册" name="register"><div class="auth-form"><van-field v-model="phone" label="手机号" placeholder="请输入 11 位手机号" type="tel" maxlength="11" autocomplete="tel"/><van-field v-model="nickName" label="昵称" placeholder="用于订单与个人中心展示" autocomplete="nickname"/><van-field v-model="password" label="设置密码" placeholder="请输入密码" type="password" autocomplete="new-password"/><van-button block type="primary" class="auth-submit" :loading="submitting" @click="register">创建会员账户</van-button></div></van-tab>
        </van-tabs>
      </section>
      <p class="auth-tip">登录即表示你同意平台服务规则。账户信息仅用于订单履约与售后服务。</p>
    </template>
    <template v-else>
      <div class="member-summary"><span class="member-avatar">{{ profile?.nickName?.slice(0, 1) || '会' }}</span><div><p class="eyebrow">MEMBER CENTER</p><h1>{{ profile?.nickName }}</h1><p>{{ profile?.phone }}</p></div><van-button size="small" plain @click="logout">退出登录</van-button></div>
      <div class="section-heading"><h2>收货地址</h2><van-button size="small" plain type="primary" @click="startNewAddress">新增地址</van-button></div>
      <div v-for="a in addresses" :key="a.id" class="address-card"><div><b>{{ a.receiverName }} · {{ a.receiverPhone }}</b><van-tag v-if="a.isDefault === 1" type="success" plain>默认</van-tag><p>{{ a.province }}{{ a.city }}{{ a.district }}{{ a.detailAddress }}</p></div><div class="address-actions"><van-button v-if="a.isDefault !== 1" size="small" plain @click="setDefault(a.id)">设为默认</van-button><van-button size="small" plain @click="startEditAddress(a)">编辑</van-button><van-button size="small" plain type="danger" @click="removeAddress(a)">删除</van-button></div></div>
      <van-empty v-if="!addresses.length" image="search" description="还没有收货地址"/>
      <section v-if="showAddressForm" class="address-editor"><h3>{{ editingAddressId ? '编辑收货地址' : '新增收货地址' }}</h3><van-cell-group inset class="address-form"><van-field v-model="address.receiverName" label="收货人" placeholder="请输入收货人姓名"/><van-field v-model="address.receiverPhone" label="联系电话" placeholder="请输入联系电话" type="tel"/><van-field v-model="address.province" label="省份" placeholder="如：吉林省"/><van-field v-model="address.city" label="城市" placeholder="如：四平市"/><van-field v-model="address.district" label="区县" placeholder="如：铁东区"/><van-field v-model="address.detailAddress" label="详细地址" placeholder="街道、门牌号等"/></van-cell-group><div class="address-editor-actions"><van-button block plain @click="cancelAddressEdit">取消</van-button><van-button block type="primary" :loading="savingAddress" @click="saveAddress">保存地址</van-button></div></section>
      <h2>收藏与足迹</h2><van-cell class="card" is-link title="查看收藏与浏览记录" :value="`收藏 ${favorites.length} · 足迹 ${history.length}`" @click="router.push('/activity')" />
    </template>
  </section>
</template>
<script setup>
import { reactive, ref, onMounted } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import { memberApi } from '../api'
import { useRoute, useRouter } from 'vue-router'
const phone = ref(''), password = ref(''), nickName = ref(''), authMode = ref('login'), submitting = ref(false)
const route = useRoute(), router = useRouter()
const logged = ref(!!localStorage.getItem('member-token')), profile = ref(null), addresses = ref([]), favorites = ref([]), history = ref([])
const address = reactive({ receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '' })
const editingAddressId = ref(null), showAddressForm = ref(false), savingAddress = ref(false)
function validCredentials(requireNickName = false) { if (!/^1\d{10}$/.test(phone.value)) { showToast('请输入正确的 11 位手机号'); return false } if (!password.value) { showToast('请输入密码'); return false } if (requireNickName && !nickName.value.trim()) { showToast('请输入昵称'); return false } return true }
async function load() { if (!logged.value) return; try { profile.value = (await memberApi.profile()).data; addresses.value = (await memberApi.addresses()).data || []; favorites.value = (await memberApi.favorites()).data.records || []; history.value = (await memberApi.browseHistory()).data.records || [] } catch (e) { showToast(e) } }
async function login() { if (!validCredentials()) return; submitting.value = true; try { const r = await memberApi.login({ phone: phone.value, password: password.value }); localStorage.setItem('member-token', r.data.token); logged.value = true; await load(); router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/') } catch (e) { showToast(e) } finally { submitting.value = false } }
async function register() { if (!validCredentials(true)) return; submitting.value = true; try { await memberApi.register({ phone: phone.value, password: password.value, nickName: nickName.value.trim() }); authMode.value = 'login'; password.value = ''; showToast('注册成功，请登录') } catch (e) { showToast(e) } finally { submitting.value = false } }
function resetAddress() { Object.keys(address).forEach(k => address[k] = '') }
function validAddress() { const labels = { receiverName: '收货人', receiverPhone: '联系电话', province: '省份', city: '城市', district: '区县', detailAddress: '详细地址' }; const missing = Object.entries(labels).find(([key]) => !address[key].trim()); if (missing) { showToast(`请输入${missing[1]}`); return false } return true }
function startNewAddress() { resetAddress(); editingAddressId.value = null; showAddressForm.value = true }
function startEditAddress(item) { Object.assign(address, { receiverName: item.receiverName, receiverPhone: item.receiverPhone, province: item.province, city: item.city, district: item.district, detailAddress: item.detailAddress }); editingAddressId.value = item.id; showAddressForm.value = true }
function cancelAddressEdit() { showAddressForm.value = false; editingAddressId.value = null; resetAddress() }
async function saveAddress() { if (!validAddress()) return; savingAddress.value = true; try { const payload = { ...address }; if (editingAddressId.value) { await memberApi.updateAddress(editingAddressId.value, payload); showToast('地址已更新') } else { await memberApi.addAddress(payload); showToast('地址已添加') } cancelAddressEdit(); await load() } catch (e) { showToast(e) } finally { savingAddress.value = false } }
async function setDefault(id) { try { await memberApi.setDefaultAddress(id); await load() } catch (e) { showToast(e) } }
async function removeAddress(item) { try { await showConfirmDialog({ title: '删除地址', message: `确定删除“${item.receiverName}”的收货地址吗？` }); await memberApi.removeAddress(item.id); showToast('地址已删除'); await load() } catch (e) { if (e !== 'cancel') showToast(e) } }
function logout() { localStorage.removeItem('member-token'); logged.value = false; profile.value = null }
onMounted(load)
</script>
