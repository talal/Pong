<template>
  <div class="container text-center" style="max-width: 400px;">
    <h1 class="mb-4">PaF Demo (SPA, JWT)</h1>

    <!-- optional note -->
    <div v-if="alreadyLoggedIn" class="alert alert-success" role="alert">
      You are already logged in.
    </div>

    <RouterLink to="/app/signup" class="btn btn-primary btn-lg d-block w-100 mb-3">Sign Up</RouterLink>
    <RouterLink to="/app/login" class="btn btn-success btn-lg d-block w-100 mb-3">Sign In</RouterLink>
    <RouterLink to="/app/hello" class="btn btn-warning btn-lg d-block w-100 mb-3">Hello World</RouterLink>
    <RouterLink to="/app/chat"  class="btn btn-warning btn-lg d-block w-100 mb-3">Chat</RouterLink>
    <RouterLink to="/app/game" class="btn btn-danger btn-lg d-block w-100 mb-3">Play Pong</RouterLink>

    <button type="button" class="btn btn-outline-secondary btn-lg d-block w-100"
            @click="logoutJwt">
      Logout
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { auth } from '../auth.js'

const route = useRoute()
const alreadyLoggedIn = computed(() => route.query.alreadyLoggedIn !== undefined)

function logoutJwt() {
  //close STOMP connection properly (if open in any tab)
  try { window.stompJwtDeactivate?.() } catch {}

  //delete token
  auth.token = null;

  alert('JWT deleted.');
}
</script>
