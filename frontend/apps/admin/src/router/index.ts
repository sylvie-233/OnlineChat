import { createRouter, createWebHistory } from 'vue-router'
import { useAdminAuthStore } from '@/stores/auth'

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
      component: () => import('@/views/layout/index.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue') },
        { path: 'users', name: 'Users', component: () => import('@/views/user/index.vue') },
        { path: 'groups', name: 'Groups', component: () => import('@/views/group/index.vue') },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const auth = useAdminAuthStore()
  if (to.matched.some(r => r.meta.requiresAuth) && !auth.token) {
    next('/login')
  } else if (to.meta.guest && auth.token) {
    next('/')
  } else {
    next()
  }
})

export default router
