param(
    [string]$Launch4jDir = "C:\Program Files (x86)\Launch4j"
)

Write-Host "=== Build VisorV2 ===" -ForegroundColor Cyan
$root = Split-Path -Parent $PSCommandPath

# 1. Compilar y empaquetar
Write-Host "[1/3] Compilando y empaquetando..." -ForegroundColor Yellow
Push-Location $root
mvn package -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: mvn package falló" -ForegroundColor Red
    Pop-Location; exit 1
}
Pop-Location

# 2. Ejecutar Launch4j
$configFile = Join-Path $root "visorv2.xml"
$launch4jc  = Join-Path $Launch4jDir "launch4jc.exe"

if (-not (Test-Path $launch4jc)) {
    Write-Host "ERROR: No se encuentra $launch4jc" -ForegroundColor Red
    Write-Host "Puedes abrir el Launch4j GUI y cargar: $configFile" -ForegroundColor Yellow
    exit 2
}

Write-Host "[2/3] Generando .exe..." -ForegroundColor Yellow
& $launch4jc $configFile
if ($LASTEXITCODE -eq 0) {
    Write-Host "[3/3] ✓ VisorV2.exe generado en D:\Descargas\VisorV2\" -ForegroundColor Green
} else {
    Write-Host "ERROR: Launch4j falló con código $LASTEXITCODE" -ForegroundColor Red
    exit 3
}
