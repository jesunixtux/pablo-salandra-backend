@echo off
setlocal EnableExtensions
rem Backend DSY1105 sobre la MariaDB de XAMPP.
rem Si XAMPP esta parado, ofrece iniciarlo con su propio mysqld (una sola instancia:
rem nunca inicia otro si el puerto ya esta ocupado). La base se crea sola al arrancar.
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

call :port_listening %DB_PORT%
if not errorlevel 1 goto server_up

rem --- MariaDB de XAMPP parada: ofrecer iniciarla ---
if not defined XAMPP_DIR set "XAMPP_DIR=C:\xampp"
set "MYSQLD=%XAMPP_DIR%\mysql\bin\mysqld.exe"
if not exist "%MYSQLD%" set "MYSQLD=%XAMPP_DIR%\mysql\bin\mariadbd.exe"
if not exist "%MYSQLD%" (
    echo ERROR: Nada escucha en %DB_HOST%:%DB_PORT% y no hay mysqld en %XAMPP_DIR%.
    echo Abre el Panel de Control de XAMPP y pulsa Start en el modulo MySQL.
    echo Si XAMPP esta en otra carpeta, ejecuta antes: set XAMPP_DIR=D:\ruta\xampp
    pause
    exit /b 1
)
echo El modulo MySQL de XAMPP no esta escuchando en el puerto %DB_PORT%.
choice /C SN /M "Iniciar la MariaDB de XAMPP ahora?"
if errorlevel 2 (
    echo Cancelado. Inicia el modulo MySQL desde el Panel de XAMPP y reintenta.
    pause
    exit /b 1
)
start "MySQL de XAMPP" /D "%XAMPP_DIR%\mysql\bin" "%MYSQLD%" --standalone --console
echo Esperando a que MariaDB acepte conexiones en el puerto %DB_PORT%...
set WAITED=0
:wait_port
call :port_listening %DB_PORT%
if not errorlevel 1 goto server_up
set /a WAITED+=1
if %WAITED% GEQ 30 (
    echo ERROR: MariaDB no abrio el puerto %DB_PORT% en 30 segundos.
    echo Revisa la ventana "MySQL de XAMPP" ^(puerto ocupado o my.ini incorrecto^).
    echo Alternativa: inicia el modulo desde el Panel de Control de XAMPP.
    pause
    exit /b 1
)
timeout /t 1 /nobreak >nul
goto wait_port

:server_up
echo MariaDB disponible en %DB_HOST%:%DB_PORT%.
echo El backend crea la base "%DB_NAME%" y sus tablas automaticamente si faltan.
echo API REST en http://127.0.0.1:8090 con las 30 cuentas ficticias del perfil dev.

set "SPRING_PROFILES_ACTIVE=dev,xampp"
call gradlew.bat bootRun
if errorlevel 1 (
    echo.
    echo ERROR: Spring Boot termino con problemas. Revisa los mensajes de arriba.
    echo Causas tipicas: credenciales incorrectas ^(DB_USER / DB_PASSWORD^) o que el
    echo usuario no tenga privilegios CREATE DATABASE sobre el servidor.
    pause
)
endlocal
exit /b 0

:port_listening
netstat -an | findstr ":%~1 " | findstr /i "LISTENING" >nul
exit /b %errorlevel%
