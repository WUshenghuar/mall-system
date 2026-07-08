import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', perm: 'dashboard', depth: 1 }
      },
      {
        path: 'product',
        redirect: '/product/spu',
        meta: { perm: 'product' },
        children: [
          {
            path: 'spu',
            name: 'ProductSpu',
            component: () => import('@/views/product/index.vue'),
            meta: { title: '商品管理', module: '商品', perm: 'product', depth: 2 }
          },
          {
            path: 'category',
            name: 'ProductCategory',
            component: () => import('@/views/product/category.vue'),
            meta: { title: '分类管理', module: '商品', perm: 'product', depth: 2 }
          },
          {
            path: 'brand',
            name: 'ProductBrand',
            component: () => import('@/views/product/brand.vue'),
            meta: { title: '品牌管理', module: '商品', perm: 'product', depth: 2 }
          },
          {
            path: 'sku/:spuId',
            name: 'ProductSku',
            component: () => import('@/views/product/sku.vue'),
            meta: { title: 'SKU 管理', module: '商品', perm: 'product', depth: 3 }
          }
        ]
      },
      {
        path: 'order',
        redirect: '/order/list',
        meta: { perm: 'order' },
        children: [
          {
            path: 'list',
            name: 'OrderList',
            component: () => import('@/views/order/index.vue'),
            meta: { title: '订单管理', module: '订单', perm: 'order', depth: 2 }
          },
          {
            path: 'detail/:id',
            name: 'OrderDetail',
            component: () => import('@/views/order/detail.vue'),
            meta: { title: '订单详情', module: '订单', perm: 'order', depth: 3 }
          },
          {
            path: 'refund',
            name: 'OrderRefund',
            component: () => import('@/views/order/refund.vue'),
            meta: { title: '退款处理', module: '订单', perm: 'order', depth: 2 }
          }
        ]
      },
      {
        path: 'member',
        name: 'Member',
        component: () => import('@/views/member/index.vue'),
        meta: { title: '会员管理', module: '会员', perm: 'member', depth: 2 }
      },
      {
        path: 'marketing',
        redirect: '/marketing/coupon',
        meta: { perm: 'marketing' },
        children: [
          {
            path: 'coupon',
            name: 'MarketingCoupon',
            component: () => import('@/views/marketing/index.vue'),
            meta: { title: '优惠券管理', module: '营销', perm: 'marketing', depth: 2 }
          },
          {
            path: 'activity',
            name: 'MarketingActivity',
            component: () => import('@/views/marketing/activity.vue'),
            meta: { title: '活动管理', module: '营销', perm: 'marketing', depth: 2 }
          }
        ]
      },
      {
        path: 'finance',
        redirect: '/finance/statement',
        meta: { perm: 'finance' },
        children: [
          {
            path: 'statement',
            name: 'FinanceStatement',
            component: () => import('@/views/finance/index.vue'),
            meta: { title: '对账单', module: '财务', perm: 'finance', depth: 2 }
          },
          {
            path: 'tax',
            name: 'FinanceTax',
            component: () => import('@/views/finance/tax.vue'),
            meta: { title: '关税配置', module: '财务', perm: 'finance', depth: 2 }
          }
        ]
      },
      {
        path: 'system',
        name: 'System',
        component: () => import('@/views/system/index.vue'),
        meta: { title: '系统设置', module: '系统', perm: 'system', depth: 2 }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Auth + Permission guard
router.beforeEach((to, _from, next) => {
  NProgress.start()
  const token = localStorage.getItem('token')

  // Allow public routes (login)
  if (to.meta.public) {
    if (token) return next('/dashboard')
    return next()
  }

  // Require token for all other routes
  if (!token) return next('/login')

  // Check permission requirement
  const requiredPerm = to.meta.perm
  if (requiredPerm) {
    try {
      const permsRaw = localStorage.getItem('permissions')
      const permissions = permsRaw ? JSON.parse(permsRaw) : []
      if (permissions.length > 0 && !permissions.includes(requiredPerm) && !permissions.includes('*')) {
        return next('/dashboard')
      }
    } catch { /* permissions not ready yet — allow */ }
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
