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
      <h2>收货地址</h2><div v-for="a in addresses" :key="a.id" class="card">{{ a.receiverName }} · {{ a.receiverPhone }}<br>{{ a.province }}{{ a.city }}{{ a.district }}{{ a.detailAddress }}</div>
      <van-cell-group inset class="address-form"><van-field v-model="address.receiverName" label="收货人"/><van-field v-model="address.receiverPhone" label="联系电话"/><van-field v-model="address.province" label="省份"/><van-field v-model="address.city" label="城市"/><van-field v-model="address.district" label="区县"/><van-field v-model="address.detailAddress" label="详细地址"/></van-cell-group>
      <van-button block type="primary" @click="addAddress">添加地址</van-button><h2>收藏与足迹</h2><div class="card">已收藏 {{ favorites.length }} 件 · 最近浏览 {{ history.length }} 件</div>
    </template>
  </section>
</template>
<script setup>
import { reactive, ref, onMounted } from 'vue'
import { showToast } from 'vant'
import { memberApi } from '../api'
const phone = ref(''), password = ref(''), nickName = ref(''), authMode = ref('login'), submitting = ref(false)
const logged = ref(!!localStorage.getItem('member-token')), profile = ref(null), addresses = ref([]), favorites = ref([]), history = ref([])
const address = reactive({ receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '' })
function validCredentials(requireNickName = false) { if (!/^1\d{10}$/.test(phone.value)) { showToast('请输入正确的 11 位手机号'); return false } if (!password.value) { showToast('请输入密码'); return false } if (requireNickName && !nickName.value.trim()) { showToast('请输入昵称'); return false } return true }
async function load() { if (!logged.value) return; try { profile.value = (await memberApi.profile()).data; addresses.value = (await memberApi.addresses()).data || []; favorites.value = (await memberApi.favorites()).data.records || []; history.value = (await memberApi.browseHistory()).data.records || [] } catch (e) { showToast(e) } }
async function login() { if (!validCredentials()) return; submitting.value = true; try { const r = await memberApi.login({ phone: phone.value, password: password.value }); localStorage.setItem('member-token', r.data.token); logged.value = true; load() } catch (e) { showToast(e) } finally { submitting.value = false } }
async function register() { if (!validCredentials(true)) return; submitting.value = true; try { await memberApi.register({ phone: phone.value, password: password.value, nickName: nickName.value.trim() }); authMode.value = 'login'; password.value = ''; showToast('注册成功，请登录') } catch (e) { showToast(e) } finally { submitting.value = false } }
async function addAddress() { try { await memberApi.addAddress(address); showToast('地址已添加'); Object.keys(address).forEach(k => address[k] = ''); load() } catch (e) { showToast(e) } }
function logout() { localStorage.removeItem('member-token'); logged.value = false; profile.value = null }
onMounted(load)
</script>
