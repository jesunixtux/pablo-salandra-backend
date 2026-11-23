@echo off
setlocal
rem Backend DSY1105 sobre la MariaDB de XAMPP (panel de control -> Start en MySQL).
rem No instala ni inicia ningun servidor: si XAMPP ya usa el puerto, aqui se detecta y se respeta.
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
    echo ERROR: Java JDK 21 no esta en el PATH.
    echo Instalalo, cierra esta ventana y vuelve a abrir el script.
    pause
    exit /b 1
)
if not exist "gradlew.bat" (
    echo ERROR: No se encontro gradlew.bat en esta carpeta.
    echo Copia este script en la raiz del repositorio del backend.
    pause
    exit /b 1
)

if not defined DB_HOST set "DB_HOST=127.0.0.1"
if not defined DB_PORT set "DB_PORT=3306"
if not defined DB_NAME set "DB_NAME=bitacora_dsy1105_dev"
if not defined DB_USER set "DB_USER=root"
if not defined DB_PASSWORD set "DB_PASSWORD="

netstat -an | findstr ":%DB_PORT% " | findstr /i "LISTENING" >nul
if errorlevel 1 (
    echo ERROR: Nada escucha en %DB_HOST%:%DB_PORT%.
    echo 1. Abre el Panel de Control de XAMPP.
    echo 2. Pulsa Start en el modulo MySQL ^(MariaDB^).
    echo 3. Si MariaDB usa otro puerto, ponlo aqui mismo en DB_PORT y reintenta.
    pause
    exit /b 1
)

echo Usando MariaDB de XAMPP en %DB_HOST%:%DB_PORT%, base "%DB_NAME%".
echo Si es el primer arranque, crea la base "%DB_NAME%" en phpMyAdmin
echo ^(utf8mb4 / utf8mb4_general_ci^). Flyway crea las tablas solo.
echo API REST en http://127.0.0.1:8090 con las 30 cuentas ficticias del perfil dev.

set "SPRING_PROFILES_ACTIVE=dev,xampp"
call gradlew.bat bootRun
if errorlevel 1 (
    echo.
    echo ERROR: Spring Boot termino con problemas. Revisa los mensajes de arriba.
    echo Causas tipicas: base "%DB_NAME%" inexistente ^(crearla en phpMyAdmin^)
    echo o credenciales incorrectas ^(DB_USER / DB_PASSWORD^).
    pause
)
endlocal
