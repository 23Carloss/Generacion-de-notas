# Lanzador de Game Maintenance para Windows

## Uso normal

Ejecuta `GameMaintenanceLauncher.exe` o haz doble clic en
`iniciar-game-maintenance.bat`.

El lanzador:

1. comprueba el backend Jetty en `http://127.0.0.1:8080` y el frontend Vite en
   `http://127.0.0.1:5173`;
2. inicia únicamente el servicio que falte;
3. abre una ventana independiente de Google Chrome (preferido) u Opera GX;
4. registra cuáles procesos fueron iniciados por él;
5. cuando se cierra esa ventana del navegador (o la ventana del lanzador),
   detiene únicamente sus propios procesos.

Los procesos que ya estaban activos antes de abrir el lanzador nunca se agregan
al grupo de procesos administrado y, por tanto, no se cierran.

Los registros se guardan en:

`%LOCALAPPDATA%\GameMaintenanceLauncher\logs`

El último registro de procesos se guarda en:

`%LOCALAPPDATA%\GameMaintenanceLauncher\last-run.txt`

## Requisitos

- JDK 25.
- Node.js y las dependencias instaladas en `game-maintenance-frontend`.
- MySQL y la base de datos local configurada para el backend.
- Google Chrome u Opera GX.

## Volver a compilar

Desde PowerShell, en la raíz del proyecto:

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\launcher\build-launcher.ps1
```

El script ejecuta las pruebas y compilaciones del backend y frontend antes de
recrear el `.exe`. Para recompilar únicamente el lanzador:

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\launcher\build-launcher.ps1 -SkipApplicationBuild
```

## Archivos fuente

- `launcher\GameMaintenanceLauncher.cs`: código fuente del ejecutable.
- `launcher\build-launcher.ps1`: script reproducible de compilación.
- `iniciar-game-maintenance.bat`: acceso alternativo para iniciar la aplicación.
