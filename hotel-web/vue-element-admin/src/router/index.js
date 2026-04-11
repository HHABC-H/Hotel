import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router)

import Layout from '@/layout'

export const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login/index'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/error-page/404'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error-page/401'),
    hidden: true
  },
  {
    path: '/',
    component: Layout,
    hidden: true,
    redirect: '/home',
    children: [
      {
        path: 'home',
        component: () => import('@/views/home/index'),
        name: 'HomeRedirect',
        meta: { title: '首页' }
      }
    ]
  },
  {
    path: '/profile',
    component: Layout,
    hidden: true,
    children: [
      {
        path: 'index',
        component: () => import('@/views/client/profile/index'),
        name: 'ProfileAlias',
        meta: { title: '个人资料', icon: 'user' }
      }
    ]
  }
]

export const asyncRoutes = [
  {
    path: '/dashboard',
    component: Layout,
    redirect: '/dashboard/index',
    children: [
      {
        path: 'index',
        component: () => import('@/views/dashboard/index'),
        name: 'Dashboard',
        meta: { title: '仪表盘', icon: 'dashboard', affix: true, roles: ['ADMIN', 'RECEPTIONIST'] }
      }
    ]
  },
  {
    path: '/room',
    component: Layout,
    redirect: '/room/list',
    name: 'Room',
    meta: { title: '客房管理', icon: 'el-icon-office-building', roles: ['ADMIN', 'RECEPTIONIST'] },
    children: [
      {
        path: 'types',
        component: () => import('@/views/room/type/index'),
        name: 'RoomTypes',
        meta: { title: '房型管理', icon: 'el-icon-collection-tag', roles: ['ADMIN', 'RECEPTIONIST'] }
      },
      {
        path: 'list',
        component: () => import('@/views/room/manage/index'),
        name: 'RoomList',
        meta: { title: '房间管理', icon: 'el-icon-house', roles: ['ADMIN', 'RECEPTIONIST'] }
      }
    ]
  },
  {
    path: '/order',
    component: Layout,
    redirect: '/order/list',
    name: 'Order',
    meta: { title: '订单管理', icon: 'el-icon-s-order', roles: ['ADMIN', 'RECEPTIONIST'] },
    children: [
      {
        path: 'list',
        component: () => import('@/views/order/manage/index'),
        name: 'OrderManage',
        meta: { title: '订单列表', icon: 'el-icon-tickets', roles: ['ADMIN', 'RECEPTIONIST'] }
      },
      {
        path: 'create',
        component: () => import('@/views/order/create/index'),
        name: 'OrderCreate',
        meta: { title: '创建订单', icon: 'el-icon-circle-plus-outline', roles: ['ADMIN', 'RECEPTIONIST'] }
      }
    ]
  },
  {
    path: '/system',
    component: Layout,
    redirect: '/system/staff',
    name: 'System',
    meta: { title: '系统管理', icon: 'el-icon-setting', roles: ['ADMIN'] },
    children: [
      {
        path: 'staff',
        component: () => import('@/views/system/user/index'),
        name: 'SystemStaff',
        meta: { title: '员工管理', icon: 'el-icon-user', roles: ['ADMIN'] }
      }
    ]
  },
  {
    path: '/staff',
    component: Layout,
    children: [
      {
        path: 'profile',
        component: () => import('@/views/client/profile/index'),
        name: 'StaffProfile',
        meta: { title: '个人中心', icon: 'el-icon-user', roles: ['ADMIN', 'RECEPTIONIST'] }
      }
    ]
  },
  {
    path: '/client',
    component: Layout,
    redirect: '/client/order-create',
    name: 'Client',
    meta: { title: '客户端', icon: 'el-icon-mobile-phone', roles: ['CLIENT'] },
    children: [
      {
        path: 'order-create',
        component: () => import('@/views/client/rooms/index'),
        name: 'ClientOrderCreate',
        meta: { title: '下单界面', icon: 'el-icon-edit-outline', roles: ['CLIENT'] }
      },
      {
        path: 'orders',
        component: () => import('@/views/client/orders/index'),
        name: 'ClientOrders',
        meta: { title: '我的订单', icon: 'el-icon-document', roles: ['CLIENT'] }
      },
      {
        path: 'profile',
        component: () => import('@/views/client/profile/index'),
        name: 'ClientProfile',
        meta: { title: '个人中心', icon: 'el-icon-user', roles: ['CLIENT'] }
      }
    ]
  },
  { path: '*', redirect: '/404', hidden: true }
]

const createRouter = () => new Router({
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})

const router = createRouter()

export function resetRouter() {
  const newRouter = createRouter()
  router.matcher = newRouter.matcher
}

export default router
