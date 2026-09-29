@echo off
setlocal
set "PROJECT_ROOT=%~dp0"

if not exist "%PROJECT_ROOT%GameMaintenanceLauncher.exe" (
  echo No se encontro GameMaintenanceLauncher.exe.
  echo Ejecuta launcher\build-launcher.ps1 para crearlo.
  pause
  exit /b 1
)

start "" "%PROJECT_ROOT%GameMaintenanceLauncher.exe"
endlocal
