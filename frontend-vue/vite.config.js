import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    //to avoid CORS trouble...
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/websocket': { target: 'ws://localhost:8080', ws: true, changeOrigin: true },
    },
  },
})
