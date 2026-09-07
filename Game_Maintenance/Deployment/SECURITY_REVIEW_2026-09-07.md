# Revisión funcional y de seguridad — Game Maintenance

Fecha: 7 de septiembre de 2026. Estado: funciones implementadas; correcciones de seguridad propuestas, pendientes de retroalimentación.

## Alcance y entorno

Se probaron los servlets, filtros, servicios y consultas JPA reales con una base H2 temporal en memoria (`AuditPU`) y cuentas ficticias. El navegador ejecutó el build de React contra esa API, escuchando exclusivamente en `127.0.0.1:18765`. La vista previa se ejecutó en `127.0.0.1:4175`.

No se conectaron las pruebas a MySQL local ni a PostgreSQL de Render. No se usaron credenciales reales, no se atacó el sitio público y no se hicieron pruebas de saturación o destrucción de datos reales. Las cadenas SQL destructivas se enviaron como datos de prueba. Los resultados de H2 no sustituyen una prueba de integración con PostgreSQL ni certifican la seguridad del despliegue público.

## Funciones implementadas

| Campo o función | Comportamiento |
|---|---|
| Buscar clientes | Máximo 120 caracteres en el navegador; búsqueda local, sin consulta SQL |
| Nombre del cliente | Máximo 100 caracteres; creación y edición, con validación de API |
| Editar cliente | Administrador puede editar nombre, teléfono y correo; el usuario común conserva acceso solo a su perfil |
| Cuenta existente | Se conservan ID, contraseña, rol y tickets; se impiden correos duplicados y quitar el correo de una cuenta con contraseña |
| Lada | Selector internacional, México +52 inicialmente, disponible en Clientes y cliente nuevo dentro de Nuevo ticket |
| Teléfono | Se compone lada + número; sin nueva columna ni migración. Un teléfono anterior no se reescribe al abrir o guardar otros datos del cliente |
| Modelo del dispositivo | Máximo 50 caracteres |
| Detalles/accesorios/daños | Máximo 150 caracteres; puede quedar vacío |
| Nombre de la pieza | Máximo 75 caracteres |
| Unidades | Entero entre 1 y 50; se rechazan fracciones en la API, sin truncarlas |
| Precio unitario | Número finito entre 0 y 50,000 MXN |
| Total de reparación | Calculado por el servidor; permite 50 × 50,000 = 2,500,000 MXN |

Los límites de dispositivo y reparación se aplican tanto al crear como al editar tickets. El formulario valida antes de crear un cliente; después de crear ese cliente, un reintento del ticket lo reutiliza. Los cambios de contacto se reflejan en los tickets mostrados. Los registros anteriores no se recortan automáticamente: al editarlos deberán cumplir los nuevos límites.

La captura de clientes administrativos acepta correo opcional para contactos sin cuenta. Añadir correo a un contacto no le asigna contraseña ni crea una sesión. El cambio de correo de una cuenta existente modifica su identificador de inicio de sesión.

## Pruebas y resultados

- 15 pruebas de integración nuevas (`Audit.ApiAuditTest`) y 5 pruebas existentes de contraseñas/limitadores.
- 3 pruebas de frontend con múltiples casos de frontera: límites, fracciones, valores vacíos, NaN, infinito y composición de teléfonos.
- Flujo real en navegador: iniciar sesión como administrador, crear y editar contacto, elegir lada, crear ticket con los máximos, editarlo y verificar restricciones del usuario común.
- Diseño del campo telefónico comprobado a 390 px de ancho.
- Build React, análisis estático y comprobación de imports con la capitalización registrada en Git.

Las pruebas `characterize...` documentan fallos actuales: que pasen significa que reprodujeron el problema, no que este esté solucionado. Se deberán cambiar para exigir la respuesta segura cuando se apruebe cada corrección.

### Protecciones comprobadas

| Prueba | Resultado observado |
|---|---|
| SQL en nombre, modelo, detalles, pieza, descripción y comentarios | Se almacena como texto, sin ejecutar la instrucción |
| SQL en reseña y descripción de imagen | Texto conservado; tablas y registros siguen accesibles |
| SQL en búsquedas DAO por correo/teléfono y login | No devuelve otras cuentas ni inicia sesión |
| Payload HTML con `onerror` en nombre | React lo muestra como texto; no se crea una etiqueta de imagen ni se ejecuta una alerta |
| Acceso anónimo/token inventado | 401 |
| Usuario modificando otro perfil o ticket | 403 |
| Usuario leyendo ticket ajeno | 403; su propio ticket sigue accesible |
| Rol administrador enviado durante registro | 400; rol en edición de contacto no cambia el rol almacenado |
| Borrado de foto con ticket incorrecto | 404; usuario común no puede borrarla (403) |
| SVG y JSON malformado/profundamente anidado | 400 |
| Petición grande con Content-Length | 413 |
| Preflight desde origen no autorizado | 403 |
| Cinco fallos de login para misma cuenta y solicitante | 429 con Retry-After |
| Edición de ticket con límites excedidos | 400 y rollback; conserva el dispositivo anterior |

No se encontró inyección SQL en las rutas y consultas ejercitadas. Las consultas revisadas emplean parámetros JPA. No se propone eliminar apóstrofos o palabras como SELECT de nombres: las consultas parametrizadas permiten tratarlos como datos legítimos. [OWASP: prevención de SQL Injection](https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html).

## Hallazgos pendientes de corrección

Las prioridades son una evaluación para esta aplicación, no puntuaciones CVSS.

### SEC-06 — Alta: contraseña literal y usuario root en configuración rastreada por Git

Evidencia estática: `Persistencia/src/main/resources/META-INF/persistence.xml` está versionado, incluye una contraseña no vacía, usuario root y acción de esquema create. El reporte omite los valores. `AppConfig` permite recurrir a ese archivo cuando faltan todas las variables DB; el Dockerfile copia los recursos al artefacto. No se comprobó si esa contraseña sigue vigente ni si hubo accesos ajenos.

Riesgo: exposición de credenciales a quienes accedan al repositorio/artefacto y uso accidental de una cuenta de base de datos con privilegios amplios.

Propuesta: rotar la credencial afectada; retirar los valores del recurso versionado; configurar secretos externos y un usuario restringido; exigir variables completas en producción y `DB_SCHEMA_ACTION=none`. Revisar el historial de Git después de rotar: borrar la línea actual no elimina las copias históricas. Una eventual reescritura del historial debe coordinarse antes de ejecutarla.

### SEC-05 — Alta: eliminar una cuenta no revoca sus sesiones activas

Reproducción: se emite un token para un administrador ficticio, otro administrador elimina esa cuenta (204) y el token eliminado todavía permite leer `/api/clientes` (200).

Causa: `ClienteServlet.doDelete` elimina la fila, pero `TokenService` conserva el ID y rol en memoria y `AuthFilter` los acepta sin consultar el estado de la cuenta. El acceso renueva el vencimiento por inactividad.

Propuesta: revocar todas las sesiones del cliente al eliminar/desactivar su cuenta; invalidarlas también ante cambios sensibles; comprobar vigencia de cuenta o una versión de sesión. Añadir expiración absoluta y limpieza periódica del almacén. Verificar que el token anterior devuelva 401 inmediatamente después del borrado.

### SEC-07 — Alta, revisión de dependencias: Jetty desactualizado

Evidencia: se empaqueta Jetty `9.4.53.v20231009`. La lista oficial publica correcciones posteriores que afectan rangos de la rama 9.4. No se ejecutaron exploits contra Jetty ni se determinó la aplicabilidad de cada aviso al proxy y a los módulos de Render.

Propuesta: planificar una actualización a una rama mantenida, verificar compatibilidad con los servlets `javax.servlet` existentes y ejecutar estas pruebas antes del despliegue. Revisar también dependencias transitivas de Java y Node con un análisis de composición de software. [Avisos oficiales de seguridad de Jetty](https://jetty.org/security.html).

### SEC-01 — Media: se evade el límite del cuerpo usando transferencia por bloques

Reproducción: un JSON válido precedido de espacios supera el límite configurado (por defecto 1,572,864 bytes). `POST /clientes` lo rechaza con Content-Length (413), pero lo procesa y crea el contacto cuando llega con `Transfer-Encoding: chunked` (201).

Causa: `RequestSizeFilter` solo compara el tamaño declarado. Cuando este se desconoce, no cuenta los bytes realmente recibidos. Los límites individuales de Jackson no cubren el tamaño acumulado de un documento.

Propuesta: imponer un límite de lectura del cuerpo completo, detener la petición al excederlo y responder 413 independientemente de la transferencia; complementar con límites y tiempos del servidor/proxy. Esta prueba demuestra el bypass local, no una caída ni un bypass confirmado del proxy de Render. [OWASP: prevención de denegación de servicio](https://cheatsheetseries.owasp.org/cheatsheets/Denial_of_Service_Cheat_Sheet.html).

### SEC-02 — Media: la validación de imagen no comprueba su contenido

Reproducción: el texto `not-an-image` codificado en Base64 y declarado como PNG se guarda como imagen (201). SVG sí se rechaza (400).

Causa: se comprueba el prefijo y tamaño Base64, pero no la firma ni que los bytes puedan decodificarse como una imagen.

Propuesta: validar firma y decodificación, imponer límites de dimensiones/píxeles antes de procesar y cuotas de adjuntos, y volver a codificar a un formato permitido. Mantener los controles de propietario, permisos y tamaño. El resultado demuestra carga de contenido falso por un administrador, no ejecución de código en el servidor. [OWASP: validación de archivos](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html).

### SEC-04 — Media: protección de login limitada a la combinación solicitante/cuenta

Reproducción: cinco intentos fallidos de la misma cuenta producen 429; ocho cuentas diferentes desde el mismo solicitante siguen llegando a autenticación y producen 401.

Causa: el limitador no dispone de una cuota agregada por solicitante; sus mapas viven en memoria del proceso. No se midió un agotamiento de memoria ni se intentó adivinar contraseñas reales.

Propuesta: límites combinados por cuenta, solicitante y servicio; expiración y tamaño máximo de mapas; protección equivalente para sesiones. Si hay varias instancias, compartir el estado del limitador. Verificar qué dirección entrega el proxy de Render antes de confiar en cabeceras reenviadas.

### SEC-03 — Baja: entradas ausentes producen 500

Reproducción: cuerpo `null` en login y registro sin `password` generan 500. La respuesta es genérica y no revela la traza.

Causa: se accede a propiedades del cuerpo o a `password.length()` antes de comprobar que existen.

Propuesta: validar cuerpo, campos obligatorios y tipos en la entrada y devolver 400 con un mensaje de validación. Conservar 500 para fallos internos y registrar su detalle únicamente en el servidor.

## Orden propuesto para la siguiente etapa

1. Retirar/rotar secretos y revocar sesiones de cuentas eliminadas.
2. Actualizar dependencias con pruebas de compatibilidad.
3. Corregir el límite real del cuerpo, validar imágenes y reforzar cuotas/limitadores.
4. Unificar validación de cuerpos ausentes y mensajes 400.
5. Repetir pruebas con PostgreSQL de staging y verificar CSP/CORS/HTTPS y configuración del proxy en el despliegue.

Ninguna de estas siete correcciones de seguridad se ha implementado en esta etapa. Sí se implementaron las validaciones necesarias para las funciones expresamente solicitadas, incluyendo respuesta 400 para nombre/teléfono inválidos al gestionar clientes y rechazo de unidades fraccionarias.
