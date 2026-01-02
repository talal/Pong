<template>
  <div class="container text-center py-5" style="max-width: 400px">
    <h1 class="display-4 mb-4">Pong</h1>

    <div v-if="isLoggedIn">
      <div class="alert alert-success" role="alert">You are logged in.</div>

      <div class="d-grid gap-3 mx-auto">
        <RouterLink to="/app/game" class="btn btn-success btn-lg py-3">Play Pong</RouterLink>
        <RouterLink to="/app/leaderboard" class="btn btn-primary btn-lg py-3">
          Leaderboard
        </RouterLink>
        <button @click="logout" class="btn btn-outline-danger">Logout</button>
      </div>
    </div>

    <div v-else>
      <div class="d-grid gap-3 mx-auto">
        <RouterLink to="/app/signup" class="btn btn-primary btn-lg d-block w-100 mb-3">
          Sign Up
        </RouterLink>
        <RouterLink to="/app/login" class="btn btn-success btn-lg d-block w-100 mb-3">
          Sign In
        </RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { auth } from '../auth.js'

const router = useRouter()

// Create a reactive state so Vue knows when to update the DOM
const isLoggedIn = ref(!!auth.token)

function logout() {
  auth.logout()
  isLoggedIn.value = false // Trigger the view update instantly
  router.push('/app/menu') // Ensure URL is correct (redundant but safe)
}
</script>
