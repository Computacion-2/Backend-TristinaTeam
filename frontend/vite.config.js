import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, import.meta.dirname, '')
  const apiBaseUrl = env.VITE_API_BASE_URL || '/api/v1'
  const apiProxyTarget = env.API_PROXY_TARGET || 'http://localhost:8080'
  const proxy = apiBaseUrl.startsWith('/') && !apiBaseUrl.startsWith('//')
    ? { [apiBaseUrl]: { target: apiProxyTarget, changeOrigin: true } }
    : {}

  return {
    plugins: [react()],
    server: { proxy },
  }
})
