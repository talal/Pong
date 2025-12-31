<template>
  <div class="container py-5" style="max-width:420px">
    <h2 class="mb-3">Login (JWT)</h2>

    <form @submit.prevent="login" novalidate>
      <div class="mb-3">
        <label class="form-label">E-Mail Address</label>
        <input v-model.trim="email" type="email" class="form-control" required autocomplete="email" />
      </div>

      <div class="mb-3">
        <label class="form-label">Password</label>
        <input v-model="password" type="password" class="form-control" required autocomplete="current-password" />
      </div>

      <button class="btn btn-primary w-100 mb-3" :disabled="busy">Login</button>
      <RouterLink to="/app/menu" class="btn btn-secondary w-100">Return to Menu</RouterLink>

      <div v-if="error" class="text-danger small mt-2" aria-live="polite">{{ error }}</div>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { auth } from '../auth.js'

const router = useRouter()
const email = ref('')
const password = ref('')
const busy = ref(false)
const error = ref('')

async function login() {
  if (busy.value) return
  busy.value = true; error.value = ''
  try {
    const resp = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type':'application/json' },
      body: JSON.stringify({ email: email.value, password: password.value })
    })

    if (!resp.ok) {
      if (resp.status === 401) { error.value = 'Bad credentials'; return }
      //other errors
      try {
        const data = await resp.json()
        error.value = data?.error || data?.message || `Error: ${resp.status}`
      } catch {
        error.value = `Error: ${resp.status} ${resp.statusText}`
      }
      return
    }

    const data = await resp.json() // { accessToken, expiresIn }
    auth.token = data.accessToken
    router.push('/app/menu')
  } catch (e) {
    //only real network/CORS problems are handled here
    error.value = 'Network error (CORS/connnection)'
  } finally {
    busy.value = false
  }
}
</script>
