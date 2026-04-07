import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,   // 监听 0.0.0.0，局域网 IP 可访问
    port: 38748,
    proxy: {
      '/api': {
        target: 'http://localhost:9090',
        changeOrigin: true,
      },
      '/lowcode': {
        target: 'http://localhost:9090',
        changeOrigin: true,
      },
    },
  },
})
