# Game Maintenance — Frontend (React + Vite)

Panel de servicio para el taller de reparación de consolas/laptops. Construido con
**React 19** y **Vite**, sin frameworks adicionales de UI (CSS propio).

## Requisitos
- Node.js 18 o superior
- npm

## Poner en marcha (desarrollo)
```bash
npm install
npm run dev
```
Abre la URL que imprime la terminal (por defecto `http://localhost:5173`).

## Compilar para producción
```bash
npm run build
```
Esto genera la carpeta `dist/` con HTML, CSS y JS ya optimizados y listos para
servirse como sitio estático (no requiere Node en el servidor).

```bash
npm run preview   # sirve dist/ localmente para probar el build
```

## Cómo desplegarlo
`dist/` es 100% estático, así que puedes:
- Subirlo a Netlify, Vercel, GitHub Pages, S3+CloudFront, etc.
- Copiar su contenido dentro de un módulo `webapp`/`src/main/webapp` de tu
  proyecto Java y servirlo con el mismo servidor de aplicaciones (Tomcat,
  GlassFish, etc.), siempre que ese servidor también exponga los endpoints
  REST descritos abajo bajo el mismo origen o con CORS habilitado.

## Conectarlo a tu backend real
Por ahora corre en **modo demo**: `src/api.js` mantiene los datos en memoria
(clientes y tickets de ejemplo) y no depende de ningún backend.

Para conectarlo a Capa_Negocio una vez que exponga una API REST sobre tus
DTOs (`ClienteDTO`, `DispositivoDTO`, `ResumenDTO`, `TrabajoDTO`):

1. Abre `src/api.js`.
2. Cambia `CONFIG.MOCK` a `false`.
3. Ajusta `CONFIG.API_BASE` a la URL real de tu API.
4. Implementa en el backend estos endpoints (o ajusta las rutas en `api.js`
   a los que definas):

   ```
   GET    /clientes
   POST   /clientes                body: ClienteDTO
   DELETE /clientes/{id}
   GET    /resumenes
   GET    /resumenes/{id}
   POST   /resumenes               body: ResumenDTO
   PUT    /resumenes/{id}/estado   body: { "estado": "En reparación" }
   DELETE /resumenes/{id}
   ```

> Nota: el campo `estado` del ticket (Recibido → En diagnóstico → En
> reparación → Listo para entrega → Entregado) es de conveniencia del front y
> **no existe todavía** en la entidad `Resumen` del backend. Si quieres
> persistirlo, agrega esa columna a `Resumen`/`ResumenDTO` y al endpoint
> `PUT /resumenes/{id}/estado`.

## Estructura del proyecto
```
src/
  main.jsx              punto de entrada de React
  App.jsx               enrutamiento (basado en hash) y estado global
  api.js                capa de datos (mock en memoria / fetch real)
  constants.js           enums (Plataforma, TipoTrabajo, Estado)
  utils.js               formateo de moneda/fecha, totales
  index.css              tokens de diseño y estilos globales
  components/
    Sidebar.jsx
    DashboardView.jsx
    TicketsView.jsx
    TicketDetailView.jsx
    NuevoTicketView.jsx
    ClientesView.jsx
    TicketCard.jsx
    Badge.jsx
    EmptyState.jsx
```
