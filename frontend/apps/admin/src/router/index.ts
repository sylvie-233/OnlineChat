import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Dashboard',
      component: () => import('@/views/dashboard/index.vue'),
    },
    {
      path: '/users',
      name: 'Users',
      component: () => import('@/views/user/index.vue'),
    },
    {
      path: '/groups',
      name: 'Groups',
      component: () => import('@/views/group/index.vue'),
    },
  ],
})

export default router
