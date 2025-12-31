import { createRouter, createWebHistory } from 'vue-router'
import { auth } from './auth.js'
import Menu from './views/Menu.vue'
import SignUp from './views/SignUp.vue'
import Login from './views/Login.vue'
import Hello from './views/Hello.vue'
import Chat from './views/Chat.vue'

const routes = [
  { path: '/', redirect: '/app/menu' },
  { path: '/app/menu', component: Menu },
  { path: '/app/signup', component: SignUp },
  { path: '/app/login', component: Login },
  { path: '/app/hello', component: Hello, meta: { requiresAuth: true } },
  { path: '/app/chat', component: Chat, meta: { requiresAuth: true } },
  { path: '/:pathMatch(.*)*', redirect: '/app/menu' } // Fallback
]

export const router = createRouter({
  //if the SPA runs under /app (in production), use createWebHistory('/app')
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !auth.token) return '/app/login'
});
