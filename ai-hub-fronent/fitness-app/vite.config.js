import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,   // 监听 0.0.0.0，局域网 IP 可访问
    port: 38747,
  },
})
