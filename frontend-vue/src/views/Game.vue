<template>
  <div class="container text-center py-4">
    <h1 class="mb-3">Pong</h1>

    <div v-if="status === 'WAITING'" class="alert alert-info">
      Waiting for an opponent...
      <br />
      <button class="btn btn-primary mt-2" @click="returnToMenu">Return to Menu</button>
    </div>
    <div v-else-if="status === 'FINISHED'" class="alert alert-warning">
      Game Over! Winner: {{ winner }}
      <br />
      <button class="btn btn-primary mt-2" @click="returnToMenu">Return to Menu</button>
    </div>
    <div v-else-if="status === 'PLAYING'" class="mb-3">
      <button class="btn btn-outline-danger" @click="returnToMenu">Quit Game</button>
    </div>

    <div class="d-flex justify-content-center align-items-center mb-4">
      <div class="d-flex align-items-center justify-content-center" style="width: 50px">
        <h2 class="m-0 text-primary vertical-text-left text-nowrap">
          {{ p1Name }}
        </h2>
      </div>

      <div class="mx-3">
        <canvas
          ref="gameCanvas"
          width="800"
          height="500"
          style="border: 4px solid #333; background: black; cursor: none; display: block"
          @mousemove="onMouseMove"
        ></canvas>
      </div>

      <div class="d-flex align-items-center justify-content-center" style="width: 50px">
        <h2 class="m-0 text-danger vertical-text-right text-nowrap">
          {{ p2Name }}
        </h2>
      </div>
    </div>

    <div class="d-flex justify-content-center">
      <div class="d-flex justify-content-between" style="width: 800px">
        <div class="text-center">
          <img
            :src="getAvatarUrl(p1Name)"
            alt="P1 Avatar"
            class="border border-primary rounded shadow"
            style="width: 200px; height: 200px; object-fit: cover"
            @error="onImgError"
          />
        </div>

        <div class="text-center">
          <img
            :src="getAvatarUrl(p2Name)"
            alt="P2 Avatar"
            class="border border-danger rounded shadow"
            style="width: 200px; height: 200px; object-fit: cover"
            @error="onImgError"
          />
        </div>
      </div>
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

function getUsernameFromToken(token) {
  if (!token) return 'Player 1'
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      window
        .atob(base64)
        .split('')
        .map(function (c) {
          return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
        })
        .join(''),
    )
    return JSON.parse(jsonPayload).sub
  } catch (e) {
    return 'Player 1'
  }
}

function getAvatarUrl(email) {
  // If it's a guest player or empty, use local default file
  if (!email || email.includes('Player')) {
    return '/default-avatar.jpg' // Path relative to 'public' folder
  }
  return `/api/user/avatar?email=${encodeURIComponent(email)}`
}

function onImgError(e) {
  // If the image fails to load (e.g., 404), switch to local default
  e.target.src = '/default-avatar.jpg'
}

onMounted(() => {
  if (!auth.token) {
    router.push('/app/login')
    return
  }

  // Set the logged-in user's name immediately
  p1Name.value = getUsernameFromToken(auth.token)

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
  if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
    e.preventDefault() // This stops the page from scrolling
    keys[e.key] = true
  }
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

  // Clamp to screen (paddle height is 15%, so center is 7.5 away from edge)
  const halfPaddleHeight = 7.5
  if (myY < halfPaddleHeight) myY = halfPaddleHeight
  if (myY > 100 - halfPaddleHeight) myY = 100 - halfPaddleHeight

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

    // Draw middle line (dashed for retro look)
    ctx.strokeStyle = '#333'
    ctx.lineWidth = 4
    ctx.setLineDash([8, 8]) // Create dashed effect
    ctx.beginPath()
    ctx.moveTo(400, 0)
    ctx.lineTo(400, 500)
    ctx.stroke()
    ctx.setLineDash([]) // Reset dash for other elements

    // Draw scores (overlay on top of canvas)
    ctx.fillStyle = '#666' // Gray color to fade into background slightly
    ctx.font = 'bold 60px "Courier New", monospace' // Monospace font gives a "retro" look
    ctx.textAlign = 'center'
    ctx.textBaseline = 'top'

    // Player 1 score (left side)
    ctx.fillText(p1Score.value, 350, 20)

    // Player 2 score (right side)
    ctx.fillText(p2Score.value, 450, 20)

    // Paddles
    ctx.fillStyle = 'white'
    ctx.fillRect(toX(0), toY(gameState.p1Y - 7.5), toX(2), toY(15)) // P1
    ctx.fillRect(toX(98), toY(gameState.p2Y - 7.5), toX(2), toY(15)) // P2

    // Ball
    ctx.fillStyle = 'white'
    ctx.beginPath()
    ctx.arc(toX(gameState.ballX), toY(gameState.ballY), 8, 0, Math.PI * 2)
    ctx.fill()
  }

  requestAnimationFrame(renderLoop)
}

function returnToMenu() {
  // Disconnect is handled automatically in onBeforeUnmount
  router.push('/app/menu')
}
</script>

<style scoped>
/* Rotates text -90 degrees (Bottom to Top) */
.vertical-text-left {
  transform: rotate(-90deg);
  transform-origin: center;
}

/* Rotates text 90 degrees (Top to Bottom) */
.vertical-text-right {
  transform: rotate(90deg);
  transform-origin: center;
}
</style>
