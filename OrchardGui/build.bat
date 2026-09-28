@echo off
setlocal
title Orchard GUI - build
cd /d "%~dp0"

echo ==============================================
echo   Orchard GUI  ^|  Fabric 1.21.11
echo ==============================================
echo.

rem ---- Java 21+ is required by Minecraft 1.21.11 / Fabric Loom
where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java was not found on PATH.
    echo         Install a Java 21 JDK, e.g. https://adoptium.net/temurin/releases/?version=21
    goto :fail
)

java -version 2>&1 | findstr /r /c:"version \"2[1-9]" /c:"version \"[3-9][0-9]" >nul
if errorlevel 1 (
    echo [WARN] Could not confirm Java 21 or newer. Your Java:
    java -version
    echo         If the build fails, install a Java 21 JDK and set JAVA_HOME to it.
    echo.
)

rem ---- Build with the Gradle wrapper (downloads Gradle, Minecraft and Fabric on first run)
call "%~dp0gradlew.bat" build --no-daemon
if errorlevel 1 (
    echo.
    echo [ERROR] Gradle build failed. See the output above.
    goto :fail
)

rem ---- Copy the finished mod jar into .\out
if not exist "out" mkdir "out"
del /q "out\*.jar" >nul 2>nul
for %%F in ("build\libs\*.jar") do (
    echo %%~nF | findstr /i /c:"-sources" >nul || copy /y "%%F" "out\" >nul
)

echo.
echo [OK] Build finished. Your mod:
for %%F in ("out\*.jar") do echo      %~dp0out\%%~nxF
echo.
echo Put it in .minecraft\mods (Fabric Loader 0.17.3+ for 1.21.11) and press Right Shift in game.
echo.
pause
exit /b 0

:fail
echo.
pause
exit /b 1
