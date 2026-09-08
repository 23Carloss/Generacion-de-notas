# Errores 500 en Tickets y Trabajos resueltos

## Qué se ha comprobado

Las dos lecturas dependen de la tabla `Resumen` y de la nueva columna `resenaPublica`.
La prueba aislada `PortfolioApiTest.missingPortfolioColumnBreaksBothReadsAndAdditiveMigrationRestoresExistingTickets`
reproduce ambos errores 500 al faltar esa columna; ejecutar la migración aditiva restaura las lecturas sin perder el ticket de prueba.

Esto explica una causa compatible con lo observado, pero **no confirma por sí solo el estado de Render**.
El mensaje genérico del navegador no distingue entre columna ausente, permisos, conexión u otro fallo del servidor.

## Comprobar y resolver en PostgreSQL de Render

1. Conecta tu editor SQL a la misma base configurada en `DB_URL` del **backend** de Render, no a MySQL local ni a otra base de pruebas.
2. Ejecuta `Deployment/portfolio-check-postgresql.sql`. Es solo lectura. La consulta final debe funcionar aun cuando no haya tickets:

   ```sql
   SELECT resenaPublica FROM Resumen WHERE 1 = 0;
   ```

3. Si indica que la columna no existe, respalda la base y ejecuta:

   ```sql
   BEGIN;
   ALTER TABLE Resumen ADD COLUMN IF NOT EXISTS resenaPublica VARCHAR(600) NULL;
   COMMIT;
   ```

   No añadas comillas dobles a los identificadores: PostgreSQL los normaliza a `resumen` y `resenapublica`, como espera el mapeo actual. Una columna creada como `"resenaPublica"` no es equivalente. Si ese es el caso, comparte el resultado del chequeo antes de renombrar o copiar datos.

4. Repite el chequeo y recarga la aplicación. Mantén `DB_SCHEMA_ACTION=none`; **no** uses `create`, no ejecutes nuevamente el inicializador del esquema y no vuelvas a importar los registros.
5. Despliega el nuevo frontend para retirar el panel general del rol Usuario y mostrar los fallos de carga como errores, no como cero tickets.

## Si la columna ya existe y la consulta funciona

Que la pantalla esté vacía no prueba que se hayan borrado los registros: antes el frontend descartaba también los clientes si fallaba la consulta de tickets (`Promise.all`). Ahora las consultas se resuelven independientemente; se muestran los clientes que sí cargaron y se avisa si no se pudo consultar su historial. Los errores no se presentan como contadores en cero.

El chequeo SQL incluye conteos de ambas tablas y el nombre de la base/esquema activos. Si los conteos son mayores que cero, los datos siguen allí y el siguiente paso es revisar el error del backend. Si son cero, comprueba primero que la conexión y el `DB_URL` del backend correspondan a la base donde importaste los registros. No repitas una importación sin verificarlo.

No sigas agregando columnas. Reproduce una petición fallida y revisa los logs del servicio backend.
Con esta actualización se registran el tipo de error y, cuando está disponible, `SQLState`, sin imprimir desde el nuevo diagnóstico consultas, contraseñas, tokens ni datos de clientes.

- `42703` en PostgreSQL: referencia a una columna inexistente; comprueba cuál y en qué esquema.
- `DB_COLUMN_MISSING`: revisa las migraciones pendientes y el esquema activo.
- Otro código: hay que diagnosticarlo con ese dato; no se debe asumir que es la misma causa.

Comparte únicamente las líneas relevantes, ocultando credenciales y cadenas de conexión. La nueva utilidad no cambia la configuración de logs del proveedor JPA existente; sus mensajes podrían requerir ocultar también datos de consultas.

La base real y Render no se han modificado desde estas pruebas locales.
