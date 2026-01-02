<template>
  <div class="container py-5 text-center">
    <h1 class="mb-4">Leaderboard</h1>

    <div v-if="loading" class="spinner-border text-primary" role="status">
      <span class="visually-hidden">Loading...</span>
    </div>

    <div v-else class="card shadow-sm mx-auto" style="max-width: 700px">
      <div class="card-body p-0">
        <table class="table table-striped table-hover mb-0">
          <thead class="table-dark">
            <tr>
              <th scope="col">#</th>
              <th scope="col">Player</th>
              <th scope="col">Win Rate</th>
              <th scope="col">Wins</th>
              <th scope="col">Played</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(player, index) in players" :key="index">
              <th scope="row">{{ index + 1 }}</th>
              <td>{{ player.username }}</td>
              <td class="fw-bold text-primary">{{ player.winRate }}</td>
              <td class="text-success">{{ player.gamesWon }}</td>
              <td>{{ player.gamesPlayed }}</td>
            </tr>
            <tr v-if="players.length === 0">
              <td colspan="5" class="text-muted">No games played yet.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="mt-4">
      <RouterLink to="/app/menu" class="btn btn-secondary">Back to Menu</RouterLink>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { auth } from '../auth.js'

const players = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const response = await auth.apiFetch('http://localhost:8080/api/leaderboard')
    if (response.ok) {
      players.value = await response.json()
    }
  } catch (e) {
    console.error('Failed to load leaderboard', e)
  } finally {
    loading.value = false
  }
})
</script>
