# Migración de MySQL local a PostgreSQL de Render

Esta migración conserva clientes, roles, hashes de contraseña, tickets,
trabajos, piezas, dispositivos e imágenes. No uses `mysqldump` como entrada de
PostgreSQL: sus sentencias SQL son específicas de MySQL.

## 1. Crear el esquema vacío en PostgreSQL

Usa la conexión **External** de PostgreSQL solo durante esta preparación local.
Define temporalmente estas variables y ejecuta el inicializador; no inicia el
servidor web:

```powershell
$env:DB_URL = 'jdbc:postgresql://<host-externo-render>:5432/<base-render>?sslmode=require'
$env:DB_USER = '<usuario-render>'
$env:DB_PASSWORD = '<contrasena-render>'
$env:DB_SCHEMA_ACTION = 'create'
& 'C:\Program Files\Java\jdk-25\bin\java.exe' -cp 'Models\target\classes;Persistencia\target\classes;Persistencia\target\dependency\*' Migration.PostgresSchemaInitializer
```

Después elimina esas cuatro variables de la sesión de PowerShell o cambia
`DB_SCHEMA_ACTION` a `none`. Todavía no habilites el frontend público.

## 2. Preparar las variables de migración localmente

Usa la URL **External** de PostgreSQL solo durante este paso local. Convierte
la dirección a JDBC y nunca guardes estas variables en Git:

```powershell
$env:MIGRATION_SOURCE_URL = 'jdbc:mysql://localhost:3306/ticketsmantenimiento?zeroDateTimeBehavior=CONVERT_TO_NULL'
$env:MIGRATION_SOURCE_USER = '<usuario-local-mysql>'
$env:MIGRATION_SOURCE_PASSWORD = '<contrasena-local-mysql>'
$env:MIGRATION_TARGET_URL = 'jdbc:postgresql://<host-externo-render>:5432/<base-render>?sslmode=require'
$env:MIGRATION_TARGET_USER = '<usuario-render>'
$env:MIGRATION_TARGET_PASSWORD = '<contrasena-render>'
```

## 3. Compilar y copiar los registros

Desde la raíz de `Game_Maintenance`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
& 'C:\Program Files\NetBeans-25\netbeans\java\maven\bin\mvn.cmd' '-Dmaven.repo.local=.m2' '-DskipTests' 'package'
& 'C:\Program Files\Java\jdk-25\bin\java.exe' -cp 'Persistencia\target\classes;Persistencia\target\dependency\*' Migration.MySqlToPostgresMigrator
```

La utilidad se detiene si PostgreSQL ya contiene registros, para evitar
duplicados. Al terminar muestra el total copiado por tabla y actualiza las
secuencias de identificadores para que los nuevos clientes y tickets continúen
con el siguiente ID correcto.

## 4. Configurar el API publicado

En Render usa la conexión **Internal** de PostgreSQL con estas variables:

```text
DB_URL=jdbc:postgresql://<host-interno-render>:5432/<base-render>?sslmode=require
DB_USER=<usuario-render>
DB_PASSWORD=<contrasena-render>
DB_SCHEMA_ACTION=none
```

Después configura el frontend y `APP_ALLOWED_ORIGINS` con su URL pública HTTPS.
