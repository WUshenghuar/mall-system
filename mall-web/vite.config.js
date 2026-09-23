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
        manualChunks(id) {
          if (id.includes('node_modules/ant-design-vue') || id.includes('node_modules/@ant-design/icons-vue')) return 'vendor-ui'
          if (id.includes('node_modules/vue') || id.includes('node_modules/pinia')) return 'vendor-vue'
          if (id.includes('node_modules/axios') || id.includes('node_modules/nprogress')) return 'vendor-http'
        }
      }
    }
  }
})
