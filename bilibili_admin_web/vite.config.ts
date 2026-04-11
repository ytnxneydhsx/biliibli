import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5174,
    proxy: {
      '^/(users|admin)': {
        target: 'http://150.158.146.80:8080',
        changeOrigin: true,
      },
    },
  },
})
