import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  server: {
    // Windows 已保留 2970–3069，避免 3000 触发 EACCES。
    port: 18103,
    proxy: {
      '/api': {
        target: 'http://localhost:18100',
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-vue': ['vue', 'vue-router', 'pinia'],
          'vendor-ui': ['ant-design-vue', '@ant-design/icons-vue'],
          'vendor-http': ['axios', 'nprogress']
        }
      }
    }
  }
})
