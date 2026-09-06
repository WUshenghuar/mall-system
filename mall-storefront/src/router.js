import { createRouter, createWebHistory } from 'vue-router'
import CatalogView from './views/CatalogView.vue'
import CartView from './views/CartView.vue'
import CheckoutView from './views/CheckoutView.vue'
import OrdersView from './views/OrdersView.vue'
import OrderDetailView from './views/OrderDetailView.vue'
import AccountView from './views/AccountView.vue'

const router = createRouter({ history: createWebHistory(), routes: [
  { path: '/', component: CatalogView }, { path: '/cart', component: CartView, meta: { requiresMember: true } },
  { path: '/checkout', component: CheckoutView, meta: { requiresMember: true } }, { path: '/orders', component: OrdersView, meta: { requiresMember: true } },
  { path: '/orders/:orderNo', component: OrderDetailView, meta: { requiresMember: true } },
  { path: '/account', component: AccountView }
] })

router.beforeEach(to => {
  if (to.matched.some(record => record.meta.requiresMember) && !localStorage.getItem('member-token')) {
    return { path: '/account', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
