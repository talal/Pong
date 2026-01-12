<template>
  <div class="container d-flex justify-content-center align-items-center vh-100">
    <div class="card p-4 shadow" style="max-width: 400px; width: 100%">
      <h4 class="mb-3 text-center">User Sign Up</h4>

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

        <div class="mb-3">
          <label class="form-label" for="avatar">Profile Picture (Optional)</label>
          <input
            id="avatar"
            type="file"
            class="form-control"
            accept="image/png, image/jpeg"
            @change="onFileChange"
          />
          <div class="form-text">
            Must be less than 1 MiB in size and smaller than 3000 by 3000 pixels.
          </div>
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
const file = ref(null)
const busy = ref(false)
const error = ref('')
const success = ref('')
const router = useRouter()

function onFileChange(e) {
  const files = e.target.files
  if (files && files.length > 0) {
    file.value = files[0]
  } else {
    file.value = null
  }
}

async function signup() {
  if (busy.value) return
  busy.value = true
  error.value = ''
  success.value = ''

  // Use FormData to send text + file
  const formData = new FormData()
  formData.append('email', email.value)
  formData.append('password', password.value)
  if (file.value) {
    formData.append('file', file.value)
  }

  try {
    const resp = await fetch('/api/auth/process_signup', {
      method: 'POST',
      // No 'Content-Type' header! Browser sets it to multipart/form-data automatically with boundary
      body: formData,
    })
    if (!resp.ok) {
      // 409 = e-mail address already in use, 400 = Bad Request
      let msg = `Error: ${resp.status}`
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
