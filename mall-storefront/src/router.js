import { createRouter, createWebHistory } from 'vue-router'
import CatalogView from './views/CatalogView.vue'
import CartView from './views/CartView.vue'
import CheckoutView from './views/CheckoutView.vue'
import OrdersView from './views/OrdersView.vue'
import AccountView from './views/AccountView.vue'

export default createRouter({ history: createWebHistory(), routes: [
  { path: '/', component: CatalogView }, { path: '/cart', component: CartView },
  { path: '/checkout', component: CheckoutView }, { path: '/orders', component: OrdersView },
  { path: '/account', component: AccountView }
] })
