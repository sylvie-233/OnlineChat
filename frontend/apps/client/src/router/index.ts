import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { guest: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'Chat',
          component: () => import('@/views/chat/index.vue'),
        },
        {
          path: 'contact',
          name: 'Contact',
          component: () => import('@/views/contact/index.vue'),
        },
        {
          path: 'group',
          name: 'Group',
          component: () => import('@/views/group/index.vue'),
        },
        {
          path: 'notifications',
          name: 'Notifications',
          component: () => import('@/views/notifications/index.vue'),
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('@/views/settings/index.vue'),
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()
  if (to.matched.some((r) => r.meta.requiresAuth) && !auth.token) {
    next('/login')
  } else if (to.meta.guest && auth.token) {
    next('/')
  } else {
    next()
  }
})

export default router
