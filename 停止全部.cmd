@echo off
setlocal EnableExtensions
set "CBEC_ROOT=%~dp0"
cd /d "%CBEC_ROOT%"

echo.
echo ================================================
echo   CBEC service stopper
echo   Stops only listeners on the project's dev ports
echo ================================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command "$ports = @(18100,18102,18103); $listeners = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $ports -contains $_.LocalPort }; $ids = @($listeners | Select-Object -ExpandProperty OwningProcess -Unique); foreach ($id in $ids) { Stop-Process -Id $id -Force -ErrorAction SilentlyContinue }; Write-Host ('Stopped listeners: ' + $ids.Count)"
docker compose down

echo.
echo CBEC services stopped.
pause
exit /b 0
