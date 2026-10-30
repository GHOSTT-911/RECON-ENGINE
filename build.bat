@echo off
setlocal
cd /d "%~dp0"
call "%~dp0scripts\setup-env.cmd"
if defined SETUP_FAILED goto :fail

echo Building and running tests...
call "%MVN%" clean package
if errorlevel 1 goto :fail

echo.
echo ==============================================================
echo  Built: target\reconengine.jar
echo  Run it anywhere with Java 17+:   java -jar target\reconengine.jar
echo ==============================================================
pause
goto :eof

:fail
echo Build failed - see messages above.
pause
exit /b 1
