@echo off
rem ------------------------------------------------------------------
rem  Shared helper: checks Java 17+ and finds (or downloads) Maven.
rem  Sets MVN to the Maven command to use. Sets SETUP_FAILED=1 on error.
rem ------------------------------------------------------------------
set "SETUP_FAILED="
set "MAVEN_VERSION=3.9.11"
set "TOOLS_DIR=%~dp0..\.tools"

rem ---- 1. Java -----------------------------------------------------
where java >nul 2>nul
if errorlevel 1 goto :nojava
set "JAVA_VER="
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VER=%%~v"
for /f "tokens=1 delims=." %%m in ("%JAVA_VER%") do set "JAVA_MAJOR=%%m"
if "%JAVA_MAJOR%"=="1" goto :oldjava
if "%JAVA_MAJOR%"=="" goto :nojava
if %JAVA_MAJOR% LSS 17 goto :oldjava
echo [OK] Java %JAVA_VER% found

rem ---- 2. Maven ----------------------------------------------------
where mvn >nul 2>nul
if not errorlevel 1 (
    set "MVN=mvn"
    echo [OK] Maven found on PATH
    goto :eof
)
if exist "%TOOLS_DIR%\apache-maven-%MAVEN_VERSION%\bin\mvn.cmd" goto :localmaven

echo [..] Maven not found - downloading Maven %MAVEN_VERSION% into .tools (one time only)...
if not exist "%TOOLS_DIR%" mkdir "%TOOLS_DIR%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $zip=Join-Path $env:TEMP 'maven-%MAVEN_VERSION%.zip'; Invoke-WebRequest -UseBasicParsing -Uri 'https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile $zip; Expand-Archive -Force -Path $zip -DestinationPath '%TOOLS_DIR%'; Remove-Item $zip"
if errorlevel 1 goto :mavenfail

:localmaven
set "MVN=%TOOLS_DIR%\apache-maven-%MAVEN_VERSION%\bin\mvn.cmd"
echo [OK] Using bundled Maven %MAVEN_VERSION%
goto :eof

:nojava
echo.
echo [ERROR] Java was not found.
echo         Install Java 21 (or newer) from https://adoptium.net/  - tick "Set JAVA_HOME" and "Add to PATH"
echo         then close this window and run the script again.
set "SETUP_FAILED=1"
goto :eof

:oldjava
echo.
echo [ERROR] Java %JAVA_VER% is too old. This project needs Java 17 or newer.
echo         Install Java 21 from https://adoptium.net/ and run again.
set "SETUP_FAILED=1"
goto :eof

:mavenfail
echo.
echo [ERROR] Could not download Maven. Check your internet connection,
echo         or install Maven yourself from https://maven.apache.org/download.cgi
set "SETUP_FAILED=1"
goto :eof
