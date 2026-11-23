@echo off
setlocal
rem Backend DSY1105 sobre un MySQL propio ya instalado y en marcha.
rem No instala MySQL ni inicia una segunda instancia: solo comprueba el servidor.
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
    echo ERROR: No hay servidor de base de datos escuchando en %DB_HOST%:%DB_PORT%.
    echo Inicia primero tu MySQL ^(servicio Windows, mysqld, etc.^).
    echo Si quien usa ese puerto es XAMPP, ejecuta en su lugar Iniciar-XAMPP.bat.
    pause
    exit /b 1
)
echo Puerto %DB_PORT% ocupado: si es XAMPP, usa Iniciar-XAMPP.bat (conector MariaDB).

echo MySQL en %DB_HOST%:%DB_PORT%, base "%DB_NAME%".
echo El backend crea la base y sus tablas automaticamente si faltan.
echo API REST en http://127.0.0.1:8090 con las 30 cuentas ficticias del perfil dev.

set "SPRING_PROFILES_ACTIVE=dev,mysql"
call gradlew.bat bootRun
if errorlevel 1 (
    echo.
    echo ERROR: Spring Boot termino con problemas. Revisa los mensajes de arriba.
    echo Causas tipicas: base "%DB_NAME%" inexistente o credenciales incorrectas
    echo ^(DB_USER / DB_PASSWORD^).
    pause
)
endlocal
