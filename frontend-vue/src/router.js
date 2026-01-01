import { createRouter, createWebHistory } from 'vue-router'
import { auth } from './auth.js'
import Login from './views/Login.vue'
import SignUp from './views/SignUp.vue'
import Menu from './views/Menu.vue'
import Game from './views/Game.vue'

const routes = [
  { path: '/', redirect: '/app/menu' },
  { path: '/app/login', component: Login },
  { path: '/app/signup', component: SignUp },
  { path: '/app/menu', component: Menu, meta: { requiresAuth: true } },
  { path: '/app/game', component: Game, meta: { requiresAuth: true } },
  { path: '/:pathMatch(.*)*', redirect: '/app/menu' }
]

const router = createRouter({
  //if the SPA runs under /app (in production), use createWebHistory('/app')
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !auth.token) {
    next('/app/login')
  } else {
    next()
  }
})

export default router
