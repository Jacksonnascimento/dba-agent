# Nome do executável
$AppName = "dba-agent"
$BuildDir = "build"

Write-Host "Limpando diretório de build..." -ForegroundColor Cyan
If (Test-Path $BuildDir) { 
    Remove-Item -Recurse -Force $BuildDir 
}
New-Item -ItemType Directory -Force -Path $BuildDir | Out-Null

Write-Host "Compilando para Windows (amd64)..." -ForegroundColor Yellow
$env:GOOS="windows"
$env:GOARCH="amd64"
go build -ldflags="-s -w" -o "$BuildDir\$AppName.exe" .

Write-Host "Compilando para Linux (amd64)..." -ForegroundColor Yellow
$env:GOOS="linux"
$env:GOARCH="amd64"
go build -ldflags="-s -w" -o "$BuildDir\$AppName-linux" .

Write-Host "Builds concluídos com sucesso! Binários disponíveis na pasta $BuildDir\" -ForegroundColor Green