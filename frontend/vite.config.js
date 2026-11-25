import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode, command, isPreview }) => {
  const env = loadEnv(mode, import.meta.dirname, '')
  const apiBaseUrl = env.VITE_API_BASE_URL || '/api/v1'
  const apiProxyTarget = env.API_PROXY_TARGET || 'http://localhost:8080'
  const proxy = apiBaseUrl.startsWith('/') && !apiBaseUrl.startsWith('//')
    ? { [apiBaseUrl]: { target: apiProxyTarget, changeOrigin: true } }
    : {}

  return {
    base: command === 'build' || isPreview ? '/cuidapet-front/' : '/',
    plugins: [react()],
    server: { proxy },
  }
})
