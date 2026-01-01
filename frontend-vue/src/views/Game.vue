<template>
  <div class="container text-center py-4">
    <h1 class="mb-3">Pong Game</h1>

    <div v-if="status === 'WAITING'" class="alert alert-info">
      Waiting for an opponent...
    </div>
    <div v-else-if="status === 'FINISHED'" class="alert alert-warning">
      Game Over! Winner: {{ winner }}
      <br>
      <button class="btn btn-primary mt-2" @click="returnToMenu">Back to Menu</button>
    </div>

    <div class="d-flex justify-content-center">
      <canvas
        ref="gameCanvas"
        width="800"
        height="500"
        style="border: 4px solid #333; background: black; cursor: none;"
        @mousemove="onMouseMove"
      ></canvas>
    </div>

    <div class="d-flex justify-content-center gap-5 mt-3 display-6">
      <div class="text-primary">{{ p1Name }}: {{ p1Score }}</div>
      <div class="text-danger">{{ p2Name }}: {{ p2Score }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { auth } from '../auth.js'
import { Client } from '@stomp/stompjs'

const router = useRouter()
const gameCanvas = ref(null)

// Game State
const status = ref('CONNECTING') // CONNECTING, WAITING, PLAYING, FINISHED
const p1Name = ref('Player 1')
const p2Name = ref('Player 2')
const p1Score = ref(0)
const p2Score = ref(0)
const winner = ref('')

// WebSocket Client
let client = null
let gameId = null
let subscription = null

// Canvas Context
let ctx = null

// Render State (Keep separate for smooth rendering)
let gameState = {
  ballX: 50, ballY: 50,
  p1Y: 50, p2Y: 50
}

onMounted(() => {
  if (!auth.token) {
    router.push('/app/login')
    return
  }
  ctx = gameCanvas.value.getContext('2d')
  connect()
  // Start the render loop
  requestAnimationFrame(renderLoop)
})

onBeforeUnmount(() => {
  if (client) client.deactivate()
})

function connect() {
  client = new Client({
    brokerURL: 'ws://localhost:8080/websocket',
    connectHeaders: { Authorization: 'Bearer ' + auth.token },
    // debug: (str) => console.log(str), // Uncomment for debugging
    reconnectDelay: 5000,
  })

  client.onConnect = () => {
    console.log('Connected to WS')

    // 1. Subscribe to private queue to wait for a match
    client.subscribe('/user/queue/match', (message) => {
      const body = JSON.parse(message.body)
      gameId = body.gameId
      console.log('Match found! Game ID:', gameId)
      startGame(gameId)
    })

    // 2. Ask to join
    status.value = 'WAITING'
    client.publish({ destination: '/app/game.join' })
  }

  client.activate()
}

function startGame(id) {
  status.value = 'PLAYING'

  // Subscribe to the specific game topic to receive updates 60x/sec
  subscription = client.subscribe(`/topic/game/${id}`, (message) => {
    const game = JSON.parse(message.body)
    updateState(game)
  })
}

function updateState(game) {
  if (game.state === 'FINISHED') {
    status.value = 'FINISHED'
    winner.value = (game.player1.score > game.player2.score) ? game.player1.username : game.player2.username
    if (subscription) subscription.unsubscribe()
  }

  // Update Reactive Scores (Vue updates the DOM)
  p1Name.value = game.player1.username
  p2Name.value = game.player2.username
  p1Score.value = game.player1.score
  p2Score.value = game.player2.score

  // Update Position Data (Canvas render loop uses this)
  gameState.ballX = game.ballX
  gameState.ballY = game.ballY
  gameState.p1Y = game.player1.y
  gameState.p2Y = game.player2.y
}

function onMouseMove(event) {
  if (status.value !== 'PLAYING') return

  const rect = gameCanvas.value.getBoundingClientRect()
  const yPx = event.clientY - rect.top

  // Convert Pixel Y (0-500) to Game Y (0-100)
  const yPercent = (yPx / 500) * 100

  // Send new position to server
  client.publish({
    destination: '/app/game.move',
    body: JSON.stringify({ y: yPercent })
  })
}

function renderLoop() {
  if (!ctx) return

  // Clear screen
  ctx.fillStyle = 'black'
  ctx.fillRect(0, 0, 800, 500)

  if (status.value === 'PLAYING') {
    // Helper to map 0-100 coordinate system to pixels
    const toX = (val) => (val / 100) * 800
    const toY = (val) => (val / 100) * 500

    // Draw Net
    ctx.strokeStyle = '#333'
    ctx.beginPath()
    ctx.moveTo(400, 0)
    ctx.lineTo(400, 500)
    ctx.stroke()

    // Draw Paddles (Assume width 2%, height 15%)
    ctx.fillStyle = 'white'

    // Player 1 (Left)
    ctx.fillRect(toX(0), toY(gameState.p1Y - 7.5), toX(2), toY(15))

    // Player 2 (Right)
    ctx.fillRect(toX(98), toY(gameState.p2Y - 7.5), toX(2), toY(15))

    // Draw Ball (Size roughly 1.5%)
    ctx.fillStyle = 'yellow'
    ctx.beginPath()
    ctx.arc(toX(gameState.ballX), toY(gameState.ballY), 8, 0, Math.PI * 2)
    ctx.fill()
  }

  requestAnimationFrame(renderLoop)
}

function returnToMenu() {
  router.push('/app/menu')
}
</script>
