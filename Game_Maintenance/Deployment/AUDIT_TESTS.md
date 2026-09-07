# Repetir las pruebas locales

Estas pruebas usan únicamente H2 en memoria. No ejecutan `ServerMain` ni el inicializador de PostgreSQL y no requieren variables DB. La dependencia H2 tiene alcance `test`: no se incorpora a las dependencias runtime del Dockerfile.

Desde Game_Maintenance, en PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'
& 'C:\Program Files\NetBeans-25\netbeans\java\maven\bin\mvn.cmd' '-Dmaven.repo.local=.m2' verify
```

Los resultados se generan en `Persistencia/target/surefire-reports`. Las pruebas `characterize...` confirman hallazgos pendientes y deben actualizarse al corregirlos.

Desde game-maintenance-frontend:

```powershell
node --test tests/validation.test.mjs
node tests/repository-audit.cjs
```

El segundo comando verifica mayúsculas de imports contra Git e informa solamente de la presencia de credenciales literales, sin mostrar sus valores.

## Navegador con API temporal

1. Después de `verify`, inicia la API de pruebas desde Game_Maintenance:

```powershell
$env:APP_ALLOWED_ORIGINS = 'http://127.0.0.1:4175'
& 'C:\Program Files\Java\jdk-25\bin\java.exe' -cp 'Persistencia\target\test-classes;Models\target\classes;Negocio\target\classes;Persistencia\target\classes;Persistencia\target\dependency\*;.m2\com\h2database\h2\2.4.240\h2-2.4.240.jar' Audit.AuditFixture
```

2. En otra consola, desde game-maintenance-frontend:

```powershell
$env:VITE_API_BASE = 'http://127.0.0.1:18765/GameMaintenance/api'
node 'C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js' run build
node node_modules/vite/bin/vite.js preview --host 127.0.0.1 --port 4175 --strictPort
```

3. Con Playwright disponible y Microsoft Edge instalado, ejecuta desde el frontend:

```powershell
node tests/browser-audit.cjs
```

Si Playwright está en un directorio de herramientas externo, configura `NODE_PATH` a ese directorio `node_modules`. No requiere agregarlo a las dependencias de producción. El script bloquea conexiones externas del navegador y usa solo cuentas de AuditFixture. Para repetir el flujo, reinicia AuditFixture; los datos se descartan al cerrar su proceso. Detén ambos servidores con Ctrl+C al terminar.

No publiques el build de esta prueba: contiene una URL de API local. En Render conserva `VITE_API_BASE` con la URL HTTPS pública y deja que Render genere su propio build.
