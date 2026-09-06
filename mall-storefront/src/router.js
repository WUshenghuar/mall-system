import { createRouter, createWebHistory } from 'vue-router'
import CatalogView from './views/CatalogView.vue'
import CartView from './views/CartView.vue'
import CheckoutView from './views/CheckoutView.vue'
import OrdersView from './views/OrdersView.vue'
import AccountView from './views/AccountView.vue'

const router = createRouter({ history: createWebHistory(), routes: [
  { path: '/', component: CatalogView }, { path: '/cart', component: CartView },
  { path: '/checkout', component: CheckoutView }, { path: '/orders', component: OrdersView },
  { path: '/account', component: AccountView }
] })

router.beforeEach(to => {
  if (['/cart', '/checkout', '/orders'].includes(to.path) && !localStorage.getItem('member-token')) {
    return { path: '/account', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
