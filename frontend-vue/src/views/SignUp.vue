<template>
  <div class="container d-flex justify-content-center align-items-center vh-100">
    <div class="card p-4 shadow" style="max-width: 400px; width: 100%">
      <h4 class="mb-3 text-center">User Sign Up (SPA)</h4>

      <form @submit.prevent="signup" novalidate>
        <div class="mb-3">
          <label class="form-label" for="email">E-Mail Address</label>
          <input
            id="email"
            v-model.trim="email"
            type="email"
            class="form-control"
            placeholder="name@example.com"
            required
            autocomplete="email"
          />
        </div>
        <div class="mb-3">
          <label class="form-label" for="password">Password</label>
          <input
            id="password"
            v-model="password"
            type="password"
            class="form-control"
            placeholder="••••••••"
            required
            autocomplete="new-password"
          />
        </div>

        <button class="btn btn-primary w-100 mb-3" :disabled="busy">Sign Up</button>
        <RouterLink to="/app/menu" class="btn btn-secondary w-100">Return to Menu</RouterLink>

        <div v-if="error" class="alert alert-danger mt-3" role="alert">{{ error }}</div>
        <div v-if="success" class="alert alert-success mt-3" role="alert">{{ success }}</div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const email = ref('')
const password = ref('')
const busy = ref(false)
const error = ref('')
const success = ref('')
const router = useRouter()

async function signup() {
  if (busy.value) return
  busy.value = true
  error.value = ''
  success.value = ''
  try {
    const resp = await fetch('/api/auth/process_signup', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: email.value, password: password.value }),
    })
    if (!resp.ok) {
      // 409 = e-mail address already in use, 400 = Bad Request
      let msg = `Fehler: ${resp.status}`
      try {
        const j = await resp.json()
        msg = j.error || j.message || msg
      } catch {}
      error.value = msg
      return
    }
    success.value = 'You signed up successfully.'
  } catch (e) {
    error.value = 'Network error'
  } finally {
    busy.value = false
  }
}
</script>
