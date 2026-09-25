import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

// Proxy /api sang backend Spring Boot: trình duyệt chỉ thấy một origin nên backend không cần bật CORS.
// M7 làm việc tương tự bằng nginx.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  // Test chạy trong jsdom; API giả lập bằng MSW ở tầng mạng nên services/api.js chạy thật.
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.js'],
    css: false,
  },
});
