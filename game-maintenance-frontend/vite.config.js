import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

function securityHeaders(apiBase) {
  const apiOrigin = new URL(apiBase).origin
  return {
    // unsafe-inline is required temporarily because existing React components
    // use inline style props. It does not allow inline scripts.
    "Content-Security-Policy": `default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; form-action 'self'; script-src 'self'; worker-src 'self' blob:; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data:; connect-src 'self' ${apiOrigin}`,
    "X-Content-Type-Options": "nosniff",
    "Referrer-Policy": "no-referrer",
    "Permissions-Policy": "camera=(), geolocation=(), microphone=()",
  }
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "")
  const apiBase = env.VITE_API_BASE || "http://localhost:8080/GameMaintenance/api"
  const headers = securityHeaders(apiBase)

  return {
    plugins: [react()],
    // La CSP no permite el preámbulo inline que necesita React Fast Refresh;
    // se desactiva HMR en desarrollo para que la misma protección aplique en
    // todos los entornos.
    server: { headers, hmr: false },
    preview: { headers },
  }
})
