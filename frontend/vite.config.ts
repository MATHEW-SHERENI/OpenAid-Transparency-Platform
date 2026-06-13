/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // In development the React app runs on :5173 and the Spring API on :8080.
    // Proxying /api to the backend makes the browser see same-origin requests,
    // so we need no CORS configuration while developing.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    // Component tests need a browser-like DOM; jsdom provides one in Node.
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
  },
})
