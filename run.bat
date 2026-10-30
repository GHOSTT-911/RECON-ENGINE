@echo off
setlocal
title ReconEngine (Java backend)
cd /d "%~dp0"

echo ==============================================================
echo   ReconEngine - Java / Spring Boot backend
echo ==============================================================
call "%~dp0scripts\setup-env.cmd"
if defined SETUP_FAILED goto :fail

if "%PORT%"=="" set "PORT=8081"

rem Opens the dashboard in your browser as soon as the server answers.
start "" /b powershell -NoProfile -Command "for($i=0;$i -lt 120;$i++){ try { Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 http://localhost:%PORT%/api/health | Out-Null; Start-Process 'http://localhost:%PORT%/'; break } catch { Start-Sleep -Seconds 2 } }"

echo.
echo Starting server on http://localhost:%PORT%/  (first run downloads libraries, ~1-2 min)
echo Press Ctrl+C in this window to stop the server.
echo.
call "%MVN%" spring-boot:run
if errorlevel 1 goto :fail
goto :eof

:fail
echo.
echo Something went wrong - see the messages above.
pause
exit /b 1
