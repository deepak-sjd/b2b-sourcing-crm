import path from "node:path";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    port: 3000,
    // The backend (see /backend) runs on 8080 by default. Proxying /api means
    // the frontend never hardcodes a host — same code works in every client's
    // environment as long as VITE_API_BASE_URL (or this proxy) points at their API.
    proxy: {
      "/api": {
        // Local dev backend runs on 8080 (CRM_SERVER_PORT env var overrides the
        // 8085 default in application.yml on this machine). Override with
        // VITE_API_PROXY_TARGET (shell env var, not .env.local - this file reads
        // process.env directly, not Vite's own .env loading) if that changes.
        target: process.env.VITE_API_PROXY_TARGET ?? "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: "dist",
    sourcemap: true,
  },
});