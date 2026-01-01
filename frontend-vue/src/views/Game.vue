<template>
  <div class="container text-center py-4">
    <h1 class="mb-3">Pong Game</h1>

    <div v-if="status === 'WAITING'" class="alert alert-info">Waiting for an opponent...</div>
    <div v-else-if="status === 'FINISHED'" class="alert alert-warning">
      Game Over! Winner: {{ winner }}
      <br />
      <button class="btn btn-primary mt-2" @click="returnToMenu">Back to Menu</button>
    </div>

    <div class="d-flex justify-content-center">
      <canvas
        ref="gameCanvas"
        width="800"
        height="500"
        style="border: 4px solid #333; background: black; cursor: none"
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
const status = ref('CONNECTING')
const p1Name = ref('Player 1')
const p2Name = ref('Player 2')
const p1Score = ref(0)
const p2Score = ref(0)
const winner = ref('')

// WebSocket
let client = null
let gameId = null
let subscription = null

// Canvas
let ctx = null

// Local State
let mySessionId = null // We need to know who we are to move the correct paddle locally
let myY = 50 // My local paddle position (0-100)
let keys = { ArrowUp: false, ArrowDown: false }

// Render State
let gameState = {
  ballX: 50,
  ballY: 50,
  p1Y: 50,
  p2Y: 50,
}

onMounted(() => {
  if (!auth.token) {
    router.push('/app/login')
    return
  }
  ctx = gameCanvas.value.getContext('2d')

  // Add Keyboard Listeners
  window.addEventListener('keydown', onKeyDown)
  window.addEventListener('keyup', onKeyUp)

  connect()
  requestAnimationFrame(renderLoop)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeyDown)
  window.removeEventListener('keyup', onKeyUp)
  if (client) client.deactivate()
})

function onKeyDown(e) {
  if (e.key === 'ArrowUp' || e.key === 'ArrowDown') keys[e.key] = true
}

function onKeyUp(e) {
  if (e.key === 'ArrowUp' || e.key === 'ArrowDown') keys[e.key] = false
}

function connect() {
  client = new Client({
    brokerURL: 'ws://localhost:8080/websocket',
    connectHeaders: { Authorization: 'Bearer ' + auth.token },
    reconnectDelay: 5000,
  })

  client.onConnect = () => {
    // Determine my session ID from the socket URL or handshake
    // Note: STOMP client doesn't explicitly expose session ID easily in all versions.
    // Simpler hack: The server sends the game object. I can check usernames.
    // But for controls, we just send "My Y". The server applies it to the sender.

    // Subscribe to matchmaking
    client.subscribe('/user/queue/match', (message) => {
      const body = JSON.parse(message.body)
      gameId = body.gameId
      console.log('Match found:', gameId)
      startGame(gameId)
    })

    status.value = 'WAITING'
    client.publish({ destination: '/app/game.join' })
  }

  client.activate()
}

function startGame(id) {
  status.value = 'PLAYING'
  subscription = client.subscribe(`/topic/game/${id}`, (message) => {
    const game = JSON.parse(message.body)
    updateState(game)
  })
}

function updateState(game) {
  if (game.state === 'FINISHED') {
    status.value = 'FINISHED'
    winner.value =
      game.player1.score > game.player2.score ? game.player1.username : game.player2.username
    if (subscription) subscription.unsubscribe()
  }

  p1Name.value = game.player1.username
  p2Name.value = game.player2.username
  p1Score.value = game.player1.score
  p2Score.value = game.player2.score

  // Sync physics
  gameState.ballX = game.ballX
  gameState.ballY = game.ballY
  gameState.p1Y = game.player1.y
  gameState.p2Y = game.player2.y
}

function updatePaddlePosition() {
  if (status.value !== 'PLAYING') return

  let changed = false
  const speed = 1.5 // Speed of paddle movement

  if (keys.ArrowUp) {
    myY -= speed
    changed = true
  }
  if (keys.ArrowDown) {
    myY += speed
    changed = true
  }

  // Clamp to screen (0-100)
  if (myY < 0) myY = 0
  if (myY > 100) myY = 100

  if (changed) {
    // Send to server
    client.publish({
      destination: '/app/game.move',
      body: JSON.stringify({ y: myY }),
    })
  }
}

function renderLoop() {
  if (!ctx) return

  // Update paddle logic every frame
  updatePaddlePosition()

  // Clear
  ctx.fillStyle = 'black'
  ctx.fillRect(0, 0, 800, 500)

  if (status.value === 'PLAYING') {
    const toX = (val) => (val / 100) * 800
    const toY = (val) => (val / 100) * 500

    // Net
    ctx.strokeStyle = '#333'
    ctx.beginPath()
    ctx.moveTo(400, 0)
    ctx.lineTo(400, 500)
    ctx.stroke()

    // Paddles
    ctx.fillStyle = 'white'
    ctx.fillRect(toX(0), toY(gameState.p1Y - 7.5), toX(2), toY(15)) // P1
    ctx.fillRect(toX(98), toY(gameState.p2Y - 7.5), toX(2), toY(15)) // P2

    // Ball
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
