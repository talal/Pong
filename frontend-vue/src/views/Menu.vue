<template>
  <div class="container text-center py-5">
    <h1 class="display-4 mb-4">Pong Menu</h1>
    <div class="d-grid gap-3 col-md-6 mx-auto">
      <RouterLink to="/app/game" class="btn btn-success btn-lg py-3"> Play Pong </RouterLink>

      <hr />

      <button @click="logout" class="btn btn-outline-danger">Logout</button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { auth } from '../auth.js'

const router = useRouter()

function logout() {
  auth.logout()
  router.push('/app/login')
}
</script>
