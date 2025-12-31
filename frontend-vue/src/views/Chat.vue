<template>
  <main id="main-content" class="container py-4">
    <h1 class="h3 mb-4 d-flex align-items-center gap-2">
      PaF Demo Chat <span class="badge bg-warning text-dark">JWT</span>
    </h1>

    <!-- JWT status notes -->
    <div id="jwtNotice" class="alert alert-warning" role="alert" v-show="!token">
      No JWT found. Please sign in first.
    </div>

    <div class="row g-3 align-items-end">
      <!-- connection -->
      <section class="col-md-6">
        <div class="d-flex gap-2 align-items-center">
          <label class="form-label mb-0" for="connectBtn">WebSocket:</label>
          <button id="connectBtn" type="button" class="btn btn-primary"
                  :disabled="connected || !token" @click="connect">
            Connect
          </button>
          <button id="disconnectBtn" type="button" class="btn btn-outline-secondary"
                  :disabled="!connected" @click="disconnect">
            Disconnect
          </button>
        </div>
      </section>

      <!-- send message -->
      <section class="col-md-6">
        <div class="input-group">
          <label class="input-group-text" for="messageInput">Message</label>
          <input id="messageInput" type="text" class="form-control"
                 placeholder="Your message here…" autocomplete="off" enterkeyhint="sendMessage"
                 v-model="text" @keydown.enter="sendMessage" />
          <button id="sendBtn" type="button" class="btn btn-success"
                  :disabled="!connected" @click="sendMessage">
            Send
          </button>
        </div>
      </section>
    </div>

    <!-- messages list -->
    <section class="row mt-4">
      <div class="col-12">
        <div id="conversation" class="card" :hidden="!connected">
          <div class="card-header">Messages</div>
          <div class="card-body p-0">
            <ul id="messagesList"
                class="list-group list-group-flush overflow-auto"
                style="max-height: 50vh;" role="log">
              <li v-for="(m,i) in messages" :key="i" class="list-group-item">
                {{ m }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <RouterLink to="/app/menu" class="btn btn-secondary w-100 mt-3">Return to Menu</RouterLink>
  </main>
</template>

<script setup>
import { ref, onBeforeUnmount, computed } from 'vue'
import { auth } from '../auth.js'

//state
const token = computed(() => auth.token)
const connected = ref(false)
const messages = ref([])
const text = ref('')

//STOMP client
let client = null

function connect() {
  if (!token.value) return
  client = new window.StompJs.Client({
    brokerURL: 'ws://localhost:8080/websocket',
    connectHeaders: { Authorization: 'Bearer ' + token.value },
    reconnectDelay: 5000,
    debug: () => {} // (msg) => console.log(msg)
  })

  client.onConnect = () => {
    connected.value = true
    messages.value = []
    client.subscribe('/topic/messages', (frame) => {
      try {
        const payload = JSON.parse(frame.body)
        messages.value.push(payload.content ?? frame.body)
      } catch {
        messages.value.push(frame.body)
      }
      // auto-scroll
      requestAnimationFrame(() => {
        const ul = document.getElementById('messagesList')
        if (ul) ul.scrollTop = ul.scrollHeight
      })
    })
  }

  client.onStompError = (frame) => {
    messages.value.push('STOMP error: ' + (frame.headers?.message || 'unknown'))
  }

  client.onWebSocketClose = () => {
    connected.value = false
  }

  client.activate()
}

function disconnect() {
  client?.deactivate()
  connected.value = false
}

function sendMessage() {
  const t = text.value?.trim()
  if (!t || !client || !connected.value) return
  client.publish({ destination: '/app/chat', body: JSON.stringify({ content: t }) })
  text.value = ''
}

onBeforeUnmount(disconnect)

//for cross-tab-logout (via localStorage)
window.addEventListener('storage', (e) => {
  if (e.key === 'jwt_logout') {
    disconnect()
  }
})

//for logout from another view
window.stompJwtDeactivate = () => { try { client?.deactivate() } catch {} }
</script>
