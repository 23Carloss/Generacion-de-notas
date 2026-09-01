# Configuración segura del backend

## 1. Rotar la credencial expuesta

La contraseña de MySQL que antes estaba en los archivos `persistence.xml` fue
versionada. Trátala como comprometida: cámbiala y no la reutilices.

## 2. Crear una cuenta de aplicación de mínimo privilegio

Conéctate a MySQL como administrador y sustituye los valores de ejemplo por un
usuario y una contraseña nuevos. La aplicación necesita operar únicamente sobre
su propia base de datos:

```sql
CREATE DATABASE IF NOT EXISTS ticketsmantenimiento;
CREATE USER 'game_maintenance_app'@'localhost' IDENTIFIED BY 'una-contraseña-larga-y-única';
GRANT SELECT, INSERT, UPDATE, DELETE ON ticketsmantenimiento.*
  TO 'game_maintenance_app'@'localhost';
FLUSH PRIVILEGES;
```

No utilices `root` para ejecutar la aplicación. Guarda la contraseña en un
gestor de secretos o en variables del sistema, nunca en Git.

## 3. Configurar el entorno local

Usa [`.env.example`](.env.example) como referencia y define estas variables en
tu terminal o configuración de ejecución del IDE: `DB_URL`, `DB_USER`,
`DB_PASSWORD`, `APP_ALLOWED_ORIGINS` y `SESSION_IDLE_TIMEOUT_SECONDS`.

El backend no carga archivos `.env` por sí mismo para evitar que una credencial
local termine siendo usada o versionada por accidente. Configura las variables
en el sistema/IDE. Por ejemplo, en PowerShell durante una sesión local:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/ticketsmantenimiento?zeroDateTimeBehavior=CONVERT_TO_NULL'
$env:DB_USER = 'game_maintenance_app'
$env:DB_PASSWORD = 'una-contraseña-larga-y-única'
$env:APP_ALLOWED_ORIGINS = 'http://localhost:5173'
$env:SESSION_IDLE_TIMEOUT_SECONDS = '900'
```

## 4. Esquema de datos de prueba

La aplicación ya no recrea la base en cada inicio. Para reconstruir
intencionalmente una base de pruebas, realiza un respaldo si hay algo que
quieras conservar. El administrador de MySQL debe conceder **temporalmente**
los permisos DDL necesarios a la cuenta de aplicación, iniciar una vez con
`DB_SCHEMA_ACTION=create`, y después revocar dichos permisos y volver a
`DB_SCHEMA_ACTION=none`:

```sql
GRANT CREATE, ALTER, DROP, INDEX, REFERENCES ON ticketsmantenimiento.*
  TO 'game_maintenance_app'@'localhost';
-- inicia el backend una vez con DB_SCHEMA_ACTION=create
REVOKE CREATE, ALTER, DROP, INDEX, REFERENCES ON ticketsmantenimiento.*
  FROM 'game_maintenance_app'@'localhost';
```

Nunca uses `create` ni permisos DDL de la aplicación en producción.

Después de registrar la primera cuenta mediante la aplicación, asígnale el
rol administrador de forma controlada desde MySQL, sustituyendo el correo:

```sql
UPDATE Cliente SET rol = 'ADMINISTRADOR' WHERE correo = 'admin@tu-dominio.local';
```

## 5. Producción

Cuando exista el dominio, cambia `APP_ALLOWED_ORIGINS` al origen HTTPS exacto,
por ejemplo `https://app.ejemplo.com`, y habilita `APP_ENABLE_HSTS=true` solo
después de comprobar que todo el sitio funciona exclusivamente con HTTPS.

Los encabezados del frontend se aplican automáticamente durante desarrollo y
previsualización con Vite. Al desplegar los archivos estáticos, configura el
servidor web o proxy inverso para entregar una CSP equivalente, además de
`X-Content-Type-Options`, `Referrer-Policy` y `Permissions-Policy`.

## 6. Historial Git

El secreto también existe en commits anteriores. Después de rotarlo, purga el
historial en una operación coordinada y avisa a cualquier clon del repositorio
para que se vuelva a clonar. No se reescribió el historial automáticamente.
