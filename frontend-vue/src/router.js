import { createRouter, createWebHistory } from 'vue-router'
import { auth } from './auth.js'
import Login from './views/Login.vue'
import SignUp from './views/SignUp.vue'
import Menu from './views/Menu.vue'
import Game from './views/Game.vue'
import Leaderboard from './views/Leaderboard.vue'

const routes = [
  { path: '/', redirect: '/app/menu' },
  { path: '/app/login', component: Login },
  { path: '/app/signup', component: SignUp },
  { path: '/app/menu', component: Menu }, // no 'requiresAuth' as this serves as homepage
  { path: '/app/game', component: Game, meta: { requiresAuth: true } },
  { path: '/app/leaderboard', component: Leaderboard, meta: { requiresAuth: true } },
  // Wildcard redirect (Must be last)
  { path: '/:pathMatch(.*)*', redirect: '/app/menu' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  // Only redirect if the specific route requires auth and we don't have a token
  if (to.meta.requiresAuth && !auth.token) {
    next('/app/login')
  } else {
    next()
  }
})

export default router
