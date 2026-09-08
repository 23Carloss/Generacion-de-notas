# Menú lateral y trabajos resueltos

## Activar esta versión conservando tus registros

1. Antes de desplegar el backend, realiza un respaldo de la base actual.
2. En tu conexión PostgreSQL de Render (por ejemplo, desde el editor SQL de pgAdmin), ejecuta el contenido de `Deployment/portfolio-postgresql.sql`:

   ```sql
   BEGIN;
   ALTER TABLE Resumen ADD COLUMN IF NOT EXISTS resenaPublica VARCHAR(600) NULL;
   COMMIT;
   ```

   Solo añade una columna opcional. No borra ni vuelve a importar clientes, tickets, fotografías o reseñas existentes. No ejecutes nuevamente el inicializador de esquema ni la migración completa de MySQL.

   Si ejecutas el backend contra MySQL local, usa `Deployment/portfolio-mysql.sql`: revisa primero si existe la columna y agrégala una sola vez. No uses `ADD COLUMN IF NOT EXISTS` en MySQL.
3. Mantén `DB_SCHEMA_ACTION=none`. Despliega el backend después de añadir la columna y luego el frontend. Conserva `VITE_API_BASE` apuntando a la API HTTPS y el origen HTTPS del frontend en `APP_ALLOWED_ORIGINS`; las nuevas rutas usan los mismos servicios.
4. Inicia sesión y abre **Menú → Trabajos resueltos**. Un usuario normal también ve una muestra en su panel inicial. Si no hay tickets en estado **Entregado**, verá un estado vacío, no ejemplos ficticios.

No se ha ejecutado esta migración sobre tu base real ni se ha publicado esta versión en Render.

## Qué ve cada persona

- La galería requiere iniciar sesión. Muestra únicamente tickets **Entregados**, categorías de plataforma y servicio, estrellas y fragmentos de reseñas revisados.
- La API de galería NO devuelve nombres, teléfonos, correos, identificadores de clientes o tickets, fechas, precios, modelos/números de serie escritos libremente, piezas, problemas, comentarios privados ni fotos. No basta con ocultarlos en pantalla: no viajan en esta respuesta.
- El dueño conserva acceso a sus tickets completos en **Mis tickets**. Ver trabajos resueltos no concede acceso al detalle de tickets de otras personas.
- Los administradores conservan su acceso privado para gestionar tickets y clientes.

## Publicar el texto de una reseña

Las estrellas se muestran anónimamente cuando el cliente califica su ticket entregado. El comentario original permanece privado; no se publica automáticamente porque podría incluir datos personales.

1. Como administrador, abre el ticket entregado y su sección **Reseña del cliente**.
2. En **Fragmento anónimo para mostrar**, copia una parte literal de la reseña (máximo 600 caracteres). Conserva el sentido de la opinión; no la reescribas ni publiques solo partes que cambien su significado.
3. Revisa que no contenga nombres de nadie, direcciones, usuarios de redes, datos de contacto u otra información identificable. Confirma la casilla y pulsa **Publicar fragmento**.
4. Usa **Retirar fragmento** para ocultarlo. Esto no elimina la reseña privada del cliente.

La API también bloquea teléfonos comunes, correos/enlaces, etiquetas HTML y palabras del nombre actual del dueño. Estas reglas son una ayuda, NO una anonimización infalible: no detectan todas las direcciones, nombres de terceros o teléfonos escritos con palabras. Si no puedes elegir un fragmento seguro y fiel, deja solo la valoración por estrellas.

Editar la reseña, editar el ticket o cambiarlo a un estado distinto de Entregado retira su fragmento publicado y requiere una nueva revisión. Si se reasigna a otro cliente, se retiran también la reseña y las estrellas del dueño anterior para no transferir información privada a la nueva cuenta. La publicación rechaza una reseña original desactualizada.

## Diseño y accesibilidad

- Menú desplegable desde la izquierda en todas las resoluciones, cierre con botón, Escape o fondo exterior; navegación con teclado, foco contenido y restaurado al cerrar.
- El menú puede desplazarse en pantallas bajas y conserva acceso a cerrar sesión.
- Formularios y tarjetas adaptables, tablas de clientes con desplazamiento interno, objetivos táctiles más amplios y menor animación cuando se solicita desde el sistema.
- No se agregan dependencias de frontend.

## Verificación local

Pruebas automatizadas de API con Jetty real y H2 en memoria mediante `AuditFixture`/`AuditPU`. No usan `DB_URL`, cuentas reales ni la base de producción.

- `mvn -Dmaven.repo.local=.m2 verify`: suite funcional y de seguridad anterior, más `PortfolioApiTest` (autenticación, separación de permisos, contrato sin datos personales, moderación, retirada, cambios de dueño, paginación y entradas inválidas).
- Frontend: `npm run build`, `npm run lint`, `node --test tests/validation.test.mjs` y `node tests/repository-audit.cjs`.
- Con el servidor aislado y la vista previa descritos en `Deployment/AUDIT_TESTS.md`, ejecutar `node tests/browser-audit.cjs` y `node tests/portfolio-responsive.cjs` (requieren Playwright y Chrome/Edge instalados).
- Las pruebas de interfaz recorren publicación/retirada reales, panel de usuario, permisos, paginación, fallos/reintento/estado vacío, menú/teclado y tamaños 320, 390, 768, 1280 y 1600 píxeles, además de orientación horizontal.

Las pruebas de navegador se realizaron en Chrome y Edge sobre Windows. Safari/iOS, Firefox y dispositivos físicos requieren verificación adicional; no se afirma haberlos probado. El esquema existente de PostgreSQL y el despliegue real tampoco se modificaron ni verificaron en vivo.

Los hallazgos de la auditoría anterior siguen documentados en `Deployment/SECURITY_REVIEW_2026-09-07.md`; sus correcciones generales permanecen pendientes de tu aprobación.
