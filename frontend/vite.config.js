import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * Vite 开发配置。
 *
 * /api 代理的作用：
 * 浏览器访问 /api/... 时，由 Vite 转发到 Spring Boot 的 8080 端口，
 * 前端代码不需要写死 http://localhost:8081。
 */
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
});
