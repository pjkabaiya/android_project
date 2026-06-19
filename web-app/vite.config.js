import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'https://android-project-gb6e.onrender.com',
      '/socket.io': {
        target: 'https://android-project-gb6e.onrender.com',
        ws: true,
      },
    },
  },
});
