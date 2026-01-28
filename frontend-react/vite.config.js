import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // Using a different port than the Vue frontend to run both at once
    port: 5174,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/websocket': { target: 'ws://localhost:8080', ws: true, changeOrigin: true },
    },
  },
})
