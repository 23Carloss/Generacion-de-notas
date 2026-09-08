# Reparar la columna de reseñas desde la conexión del backend

Esta opción resuelve el error `SQLState=42703: column resenapublica does not exist` sin depender de la conexión del editor SQL. Usa exactamente las mismas propiedades JDBC (`DB_URL`, `DB_USER`, `DB_PASSWORD`) con las que se inicia JPA.

## Activación controlada

1. Respalda la base actual. Publica el código actualizado de este proyecto en tu repositorio de despliegue (incluye `Migration/PortfolioSchemaRepair.java`, `AppConfig.java` y `ManejadorConexiones.java`). No basta con añadir la variable a una versión anterior.
2. En Render, abre el **Web Service game-maintenance-api → Environment**.
3. Mantén las credenciales actuales y configura:

   ```text
   DB_SCHEMA_ACTION=none
   APP_REPAIR_PORTFOLIO_SCHEMA=true
   ```

4. Guarda y despliega el backend actualizado. Antes de iniciar JPA, resolverá la tabla `resumen` del esquema activo de su propia conexión y añadirá únicamente la columna nullable `resenapublica VARCHAR(600)` si falta.
5. Espera este mensaje en los logs:

   ```text
   PORTFOLIO_SCHEMA_READY
   ```

6. Recarga Tickets y Trabajos resueltos. Después cambia `APP_REPAIR_PORTFOLIO_SCHEMA=false` y guarda/despliega otra vez. Mantén `DB_SCHEMA_ACTION=none`.

## Garantías y límites

- Desactivada por defecto: no conecta ni modifica el esquema salvo activación explícita.
- No crea bases o tablas, no elimina ni importa registros, no reescribe reseñas existentes. Es repetible.
- Solo PostgreSQL. Rechaza `DB_SCHEMA_ACTION=create` y la configuración local implícita de `persistence.xml`.
- La transacción verifica el resultado y hace COMMIT explícito; si falla revierte sus cambios y detiene el arranque con `PORTFOLIO_SCHEMA_REPAIR_FAILED`.
- Bloqueo de tabla limitado a 10 segundos de espera y sentencias a 30 segundos. Hazlo en un momento de poco uso. El usuario de BD necesita permisos para alterar esa tabla.
- Si la columna ya existe con un tipo incompatible no se cambia ni se borra automáticamente: se informa el fallo.
- La reparación sigue `DB_URL`; comprueba que ese valor siga apuntando a la base correcta. No sustituye registros que realmente estén ausentes.
- No se activa ni se ejecuta en Render desde esta tarea. La activación de la variable autoriza esa única migración al desplegar.

Prueba incluida: `PortfolioSchemaRepairTest`. La integración real usa únicamente un clúster PostgreSQL local desechable en `127.0.0.1:18766`, habilitado con `GM_TEST_PG_URL=jdbc:postgresql://127.0.0.1:18766/postgres`, y verifica esquema activo, conservación de registros, repetición y fallos seguros. Sin esa variable la prueba de integración se omite.
