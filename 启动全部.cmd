@echo off
setlocal EnableExtensions
chcp 65001 >nul
title CBEC 全部服务启动器

set "ROOT=%~dp0"
cd /d "%ROOT%"

echo.
echo ================================================
echo   CBEC 全部服务启动器
echo   基础设施 + AI 客服 + Java 后端 + B 端 + C 端
echo ================================================
echo.

where docker >nul 2>nul || goto :missingDocker
where mvn >nul 2>nul || goto :missingMaven
where npm >nul 2>nul || goto :missingNpm

echo [1/5] 启动 Docker 基础设施...
docker compose up -d
if errorlevel 1 goto :dockerFailed

if not exist "%ROOT%mall-web\node_modules" (
  echo [准备] 安装 B 端依赖...
  pushd "%ROOT%mall-web"
  call npm install || goto :npmFailed
  popd
)
if not exist "%ROOT%mall-storefront\node_modules" (
  echo [准备] 安装 C 端依赖...
  pushd "%ROOT%mall-storefront"
  call npm install || goto :npmFailed
  popd
)

echo [2/4] 启动 Java 后端（http://localhost:18100；AI 客服容器为 :18101）...
call :portInUse 18100
if errorlevel 1 (
  start "CBEC 后端 - :18100" cmd /k "cd /d ""%ROOT%"" ^&^& mvn spring-boot:run -Dspring-boot.run.profiles=dev"
) else (
  echo       :18100 已有后端运行，直接复用；请不要再在其他窗口执行 mvn spring-boot:run。
)

echo [3/4] 启动 B 端（http://localhost:18103）...
call :portInUse 18103
if errorlevel 1 (
  start "CBEC B端 - :18103" cmd /k "cd /d ""%ROOT%mall-web"" ^&^& npm run dev"
) else (
  echo       :18103 已被占用，跳过重复启动。
)

echo [4/4] 启动 C 端（http://localhost:18102）...
call :portInUse 18102
if errorlevel 1 (
  start "CBEC C端 - :18102" cmd /k "cd /d ""%ROOT%mall-storefront"" ^&^& npm run dev"
) else (
  echo       :18102 已被占用，跳过重复启动。
)

echo.
echo 服务窗口已打开。后端完成启动后，按任意键打开页面：
echo   B 端：http://localhost:18103
echo   C 端：http://localhost:18102
pause >nul
start "" http://localhost:18103
start "" http://localhost:18102
exit /b 0

:portInUse
powershell -NoProfile -Command "$listener = Get-NetTCPConnection -LocalPort %~1 -State Listen -ErrorAction SilentlyContinue; if ($listener) { exit 0 } else { exit 1 }" >nul 2>nul
exit /b %errorlevel%

:missingDocker
echo [错误] 未找到 Docker。请先启动 Docker Desktop。
goto :failed
:missingMaven
echo [错误] 未找到 Maven（mvn）。请检查 Java 23 与 Maven 环境变量。
goto :failed
:missingNpm
echo [错误] 未找到 Node.js/npm。请安装 Node.js 后重试。
goto :failed
:dockerFailed
echo [错误] Docker 基础设施未能启动，请检查 Docker Desktop 状态。
goto :failed
:npmFailed
echo [错误] 前端依赖安装失败。
:failed
pause
exit /b 1
