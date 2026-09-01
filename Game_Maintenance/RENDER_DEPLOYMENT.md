# Despliegue del backend en Render

Este repositorio se publica como un **Web Service** Docker mediante
[`render.yaml`](render.yaml). No contiene contraseñas: Render las solicitará al
crear el Blueprint.

## Valores requeridos

Configura los secretos en Render, no en `persistence.xml` ni en Git:

| Variable | Valor en Render |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://<host-interno-render>:5432/<base-render>?sslmode=require` |
| `DB_USER` | Usuario proporcionado por Render PostgreSQL |
| `DB_PASSWORD` | Contraseña proporcionada por Render PostgreSQL |
| `APP_ALLOWED_ORIGINS` | URL HTTPS exacta del sitio web, por ejemplo `https://game-maintenance-web.onrender.com` |
| `DB_SCHEMA_ACTION` | `none` |
| `APP_ENABLE_HSTS` | `true` |

`PORT` se configura automáticamente en el Blueprint. El servidor usa ese
puerto y mantiene `8080` únicamente para desarrollo local.

## Orden de publicación

1. Crea primero Render PostgreSQL en la misma región y sigue
   [`Deployment/POSTGRESQL_MIGRATION.md`](Deployment/POSTGRESQL_MIGRATION.md)
   para trasladar los registros locales.
2. Importa este repositorio como Blueprint y proporciona los cuatro secretos
   usando la conexión **Internal** de PostgreSQL.
3. Copia la URL pública HTTPS que Render asigne al API.
4. Configura esa URL en `VITE_API_BASE` del Blueprint del frontend, con el
   sufijo `/GameMaintenance/api`.
5. Actualiza `APP_ALLOWED_ORIGINS` con la URL final exacta del frontend y
   vuelve a desplegar el API.

La instancia gratuita de PostgreSQL es temporal; realiza respaldos y considera
subirla de plan antes de que concluya su periodo gratuito.
