@echo off
setlocal
rem Backend DSY1105 en modo independiente: H2 guardado en data\bitacora.mv.db.
rem No necesita MySQL ni XAMPP. Datos de demostracion del perfil dev incluidos.
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

netstat -an | findstr ":8090 " | findstr /i "LISTENING" >nul
if not errorlevel 1 (
    echo ERROR: Algo ya escucha en el puerto 8090 ^(probablemente el backend ya esta iniciado^).
    echo Cierra esa ventana antes de volver a ejecutar este script.
    pause
    exit /b 1
)

echo Base H2 persistente en "%CD%\data\bitacora.mv.db" (se crea sola al primer arranque).
echo Los registros se conservan al reiniciar. Para empezar de cero, borra la carpeta data.
echo API REST en http://127.0.0.1:8090 con las 30 cuentas ficticias del perfil dev.

if not defined DB_PATH set "DB_PATH=./data/bitacora"
set "SPRING_PROFILES_ACTIVE=dev,standalone"
call gradlew.bat bootRun
if errorlevel 1 (
    echo.
    echo ERROR: Spring Boot termino con problemas. Revisa los mensajes de arriba.
    pause
)
endlocal
