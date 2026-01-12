<template>
  <div class="container d-flex justify-content-center align-items-center vh-100">
    <div class="card p-4 shadow" style="max-width: 400px; width: 100%">
      <h4 class="mb-3 text-center">Change Password</h4>

      <form @submit.prevent="changePassword" novalidate>
        <div class="mb-3">
          <label class="form-label" for="oldPassword">Old Password</label>
          <input
            id="oldPassword"
            v-model="oldPassword"
            type="password"
            class="form-control"
            required
          />
        </div>
        <div class="mb-3">
          <label class="form-label" for="newPassword">New Password</label>
          <input
            id="newPassword"
            v-model="newPassword"
            type="password"
            class="form-control"
            required
          />
        </div>

        <button class="btn btn-warning w-100 mb-3" :disabled="busy">Change Password</button>
        <RouterLink to="/app/menu" class="btn btn-secondary w-100">Return to Menu</RouterLink>

        <div v-if="error" class="alert alert-danger mt-3" role="alert">{{ error }}</div>
        <div v-if="success" class="alert alert-success mt-3" role="alert">{{ success }}</div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { auth } from '../auth.js'

const oldPassword = ref('')
const newPassword = ref('')
const busy = ref(false)
const error = ref('')
const success = ref('')

async function changePassword() {
  if (busy.value) return
  busy.value = true
  error.value = ''
  success.value = ''

  try {
    const resp = await fetch('/api/user/change_password', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ' + auth.token,
      },
      body: JSON.stringify({
        oldPassword: oldPassword.value,
        newPassword: newPassword.value,
      }),
    })

    if (!resp.ok) {
      let msg = 'Error changing password'
      try {
        const j = await resp.json()
        msg = j.error || msg
      } catch {}
      error.value = msg
      return
    }

    success.value = 'Password changed successfully.'
    oldPassword.value = ''
    newPassword.value = ''
  } catch (e) {
    error.value = 'Network error'
  } finally {
    busy.value = false
  }
}
</script>
