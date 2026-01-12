<template>
  <div class="container d-flex justify-content-center align-items-center vh-100">
    <div class="card p-4 shadow" style="max-width: 400px; width: 100%">
      <h4 class="mb-3 text-center">Change Profile Picture</h4>

      <form @submit.prevent="uploadAvatar" novalidate>
        <div class="mb-3">
          <label class="form-label" for="avatar">New Picture</label>
          <input
            id="avatar"
            type="file"
            class="form-control"
            accept="image/png, image/jpeg"
            @change="onFileChange"
            required
          />
          <div class="form-text">
            Must be less than 1 MiB in size and smaller than 3000 by 3000 pixels.
          </div>
        </div>

        <button class="btn btn-primary w-100 mb-3" :disabled="busy || !file">Upload</button>
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

const file = ref(null)
const busy = ref(false)
const error = ref('')
const success = ref('')

function onFileChange(e) {
  const files = e.target.files
  if (files && files.length > 0) {
    file.value = files[0]
  } else {
    file.value = null
  }
}

async function uploadAvatar() {
  if (busy.value || !file.value) return
  busy.value = true
  error.value = ''
  success.value = ''

  const formData = new FormData()
  formData.append('file', file.value)

  try {
    const resp = await fetch('/api/user/avatar', {
      method: 'POST',
      headers: {
        Authorization: 'Bearer ' + auth.token,
      },
      body: formData,
    })

    if (!resp.ok) {
      let msg = 'Error uploading image'
      try {
        const j = await resp.json()
        msg = j.error || msg
      } catch {}
      error.value = msg
      return
    }

    success.value = 'Avatar updated successfully.'
    file.value = null
  } catch (e) {
    error.value = 'Network error'
  } finally {
    busy.value = false
  }
}
</script>
