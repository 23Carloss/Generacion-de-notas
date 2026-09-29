[CmdletBinding()]
param(
    [switch]$SkipApplicationBuild
)

$ErrorActionPreference = 'Stop'
$launcherDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $launcherDirectory
$backendRoot = Join-Path $projectRoot 'Game_Maintenance'
$frontendRoot = Join-Path $projectRoot 'game-maintenance-frontend'
$source = Join-Path $launcherDirectory 'GameMaintenanceLauncher.cs'
$output = Join-Path $projectRoot 'GameMaintenanceLauncher.exe'

if (-not (Test-Path -LiteralPath $backendRoot)) {
    throw "No se encontró el backend: $backendRoot"
}
if (-not (Test-Path -LiteralPath $frontendRoot)) {
    throw "No se encontró el frontend: $frontendRoot"
}

if (-not $SkipApplicationBuild) {
    $mavenCandidates = @(
        (Get-Command mvn.cmd -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -ErrorAction SilentlyContinue),
        'C:\Program Files\NetBeans-25\netbeans\java\maven\bin\mvn.cmd'
    ) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

    $maven = $mavenCandidates | Select-Object -First 1
    if (-not $maven) {
        throw 'No se encontró Maven. Instálalo o usa el Maven incluido con NetBeans 25.'
    }

    $jdk25 = 'C:\Program Files\Java\jdk-25'
    if (-not (Test-Path -LiteralPath (Join-Path $jdk25 'bin\java.exe'))) {
        throw 'No se encontró JDK 25 en C:\Program Files\Java\jdk-25.'
    }

    Write-Host 'Compilando y probando el backend...'
    $previousJavaHome = $env:JAVA_HOME
    try {
        $env:JAVA_HOME = $jdk25
        & $maven -f (Join-Path $backendRoot 'pom.xml') clean package
        if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación del backend.' }
    }
    finally {
        $env:JAVA_HOME = $previousJavaHome
    }

    if (-not (Test-Path -LiteralPath (Join-Path $frontendRoot 'node_modules\vite\bin\vite.js'))) {
        Write-Host 'Instalando dependencias del frontend...'
        & npm.cmd --prefix $frontendRoot ci
        if ($LASTEXITCODE -ne 0) { throw 'Falló la instalación del frontend.' }
    }

    Write-Host 'Probando y compilando el frontend...'
    & node.exe --test (Join-Path $frontendRoot 'tests\validation.test.mjs')
    if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas del frontend.' }
    & npm.cmd --prefix $frontendRoot run build
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación del frontend.' }
}

$compilerCandidates = @(
    'C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe',
    'C:\Windows\Microsoft.NET\Framework\v4.0.30319\csc.exe'
)
$compiler = $compilerCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $compiler) {
    throw 'No se encontró el compilador de .NET Framework incluido con Windows.'
}

Write-Host 'Creando GameMaintenanceLauncher.exe...'
& $compiler /nologo /target:winexe /optimize+ /platform:anycpu `
    /reference:System.dll `
    /reference:System.Core.dll `
    /reference:System.Drawing.dll `
    /reference:System.Management.dll `
    /reference:System.Windows.Forms.dll `
    "/out:$output" $source

if ($LASTEXITCODE -ne 0) {
    throw 'Falló la creación del ejecutable.'
}

Write-Host "Listo: $output"
