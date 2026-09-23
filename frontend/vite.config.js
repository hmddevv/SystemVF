import { defineConfig } from 'vite';
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
});
