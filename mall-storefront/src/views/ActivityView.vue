<template>
  <section class="page activity-page"><van-nav-bar title="收藏与足迹" left-text="返回" left-arrow @click-left="router.back" /><van-tabs v-model:active="tab"><van-tab title="我的收藏" name="favorite"><activity-list :items="favorites" empty="还没有收藏商品" /></van-tab><van-tab title="浏览足迹" name="history"><activity-list :items="history" empty="还没有浏览记录" /></van-tab></van-tabs></section>
</template>
<script setup>
import { defineComponent, h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Empty, showToast } from 'vant'
import { memberApi, storeApi } from '../api'
const router = useRouter(), tab = ref('favorite'), favorites = ref([]), history = ref([])
async function enrich(items) { return Promise.all(items.map(async item => { try { const product = (await storeApi.detail(item.spuId)).data?.spu; return { ...item, title: product?.spuName || `商品 #${item.spuId}` } } catch { return { ...item, title: `商品 #${item.spuId}` } }})) }
onMounted(async () => { try { const [fav, hist] = await Promise.all([memberApi.favorites({ page: 1, size: 30 }), memberApi.browseHistory({ page: 1, size: 30 })]); favorites.value = await enrich(fav.data?.records || []); history.value = await enrich(hist.data?.records || []) } catch (e) { showToast(e) } })
const ActivityList = defineComponent({ props: ['items', 'empty'], setup(props) { return () => props.items?.length ? h('div', { class: 'activity-list' }, props.items.map(item => h('div', { class: 'card activity-card', key: item.id }, [h('b', item.title), h('p', { class: 'note' }, item.createTime || '')]))) : h(Empty, { description: props.empty }) } })
</script>
<style scoped>.activity-page{padding-bottom:88px}.activity-list{padding-top:12px}.activity-card{margin-bottom:12px}.activity-card p{margin:8px 0 0}@media (min-width:1025px){.activity-page{padding-bottom:24px;max-width:760px;margin:0 auto}}</style>
