<template>
    <div v-html="html"></div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { auth } from '../auth'
import { useRouter } from 'vue-router'
const router = useRouter()
const html = ref('Loading...')

async function load() {
  const r = await auth.apiFetch('/api/hello')
  if (!r.ok) { html.value = `Fehler: ${r.status} ${r.statusText}`; return }
  html.value = await r.text() //server endpoint delivers text/html
}

onMounted(load)
</script>
