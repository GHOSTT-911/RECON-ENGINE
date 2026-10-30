@echo off
setlocal
title ReconEngine (Java backend - MySQL)
cd /d "%~dp0"

echo ==============================================================
echo   ReconEngine - running against MySQL
echo   (the database "reconengine" is created automatically)
echo ==============================================================
call "%~dp0scripts\setup-env.cmd"
if defined SETUP_FAILED goto :fail

if "%DB_USERNAME%"=="" set /p "DB_USERNAME=MySQL username [root]: "
if "%DB_USERNAME%"=="" set "DB_USERNAME=root"
if "%DB_PASSWORD%"=="" set /p "DB_PASSWORD=MySQL password for %DB_USERNAME%: "
if "%PORT%"=="" set "PORT=8081"

start "" /b powershell -NoProfile -Command "for($i=0;$i -lt 120;$i++){ try { Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 http://localhost:%PORT%/api/health | Out-Null; Start-Process 'http://localhost:%PORT%/'; break } catch { Start-Sleep -Seconds 2 } }"

call "%MVN%" spring-boot:run -Dspring-boot.run.profiles=mysql
if errorlevel 1 goto :fail
goto :eof

:fail
echo.
echo If you see "Access denied for user", the MySQL username/password is wrong.
echo Tip: just use run.bat instead - it needs no database setup at all.
pause
exit /b 1
