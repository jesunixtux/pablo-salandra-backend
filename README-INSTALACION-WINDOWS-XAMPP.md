# Instalación en Windows con XAMPP (Backend Bitácora DSY1105)

Guía para ejecutar el backend en computadores de la universidad que ya tienen XAMPP
(MariaDB). No se necesita instalar MySQL ni tocar la aplicación Android. La API
escucha siempre en `http://127.0.0.1:8090`.

## Requisitos

- **JDK 21** (`java -version` debe responder; si no, instálalo y reabre la consola).
- **XAMPP** con el módulo **MySQL (MariaDB)** iniciado desde su panel de control.
- El repositorio del backend descargado o clonado (la carpeta donde está `gradlew.bat`).

## Opción rápida: doble clic

Ejecuta `Iniciar-XAMPP.bat` desde la raíz del repositorio. El script comprueba Java,
que el puerto 3306 esté escuchando (si XAMPP lo usa, **no** inicia un segundo MySQL)
y arranca Spring Boot con los perfiles `dev,xampp`: 30 cuentas ficticias incluidas.
Si algo falla, muestra el error y queda en pantalla.

## Pasos manuales

### 1. Iniciar XAMPP

1. Abre el **Panel de Control de XAMPP**.
2. En la fila **MySQL (MariaDB)** pulsa **Start**. Debe quedar en verde y escuchar
   en el puerto **3306** (o el que configures).
3. Si el puerto está ocupado por otro MySQL, cambia el puerto de MariaDB en
   `my.ini` o usa el tuyo propio con `DB_PORT`.

### 2. Crear la base de datos

1. Entra a `http://localhost/phpmyadmin`.
2. Pestaña **Bases de datos**: crea `bitacora_dsy1105_dev` con cotejamiento
   `utf8mb4_general_ci` (el predeterminado de XAMPP sirve).
3. **No** crees tablas: Flyway las crea/actualiza solo al arrancar el backend.

Para una base compartida sin datos ficticios, usa el nombre `bitacora` y omite el
perfil `dev`.

### 3. Credenciales

XAMPP usa `root` sin contraseña, que es el valor predeterminado. Para cambiar algo,
define variables de entorno antes de ejecutar (en `cmd`):

```bat
set DB_HOST=127.0.0.1
set DB_PORT=3306
set DB_NAME=bitacora_dsy1105_dev
set DB_USER=root
set DB_PASSWORD=
```

También puedes dar la URL completa con `DB_URL=jdbc:mariadb://127.0.0.1:3306/mi_base`.

### 4. Ejecutar Spring Boot

```bat
cd ruta\al\repositorio
set SPRING_PROFILES_ACTIVE=dev,xampp
gradlew.bat bootRun
```

Primer arranque exitoso: Flyway aplica las migraciones V1/V2, Hibernate valida el
esquema y se cargan las 30 cuentas ficticias (`cuentaa-de-ejemplos.txt`, contraseña
común `DemoDSY1105!`). Los reinicios siguientes no duplican ni sobrescriben datos,
aunque hayas modificado, enviado, revisado o cerrado los registros de demo.

## Sin XAMPP ni MySQL: modo independiente

Si el equipo no puede usar MariaDB, ejecuta `Iniciar-Standalone.bat`: usa H2
guardado en `data\bitacora.mv.db` dentro del repositorio, conserva los registros al
reiniciar y también carga la demo (`dev,standalone`). Para empezar de cero, borra la
carpeta `data`. Es solo para demostraciones académicas y desarrollo local.

`Iniciar-MySQL.bat` es equivalente para un MySQL propio ya instalado y en marcha
(perfil `dev,mysql`).

## Verificación rápida

```bat
curl http://127.0.0.1:8090/api/auth/login -X POST -H "Content-Type: application/json" ^
 -d "{\"email\":\"alba.5a@demo.dsy1105.test\",\"password\":\"DemoDSY1105!\"}"
```

Devuelve `token` → autenticación y base funcionando. Configura la app Android con la
misma URL (`10.0.2.2:8090` en el emulador).

## Solución de problemas

| Mensaje | Causa / arreglo |
| --- | --- |
| `Access denied for user 'root'` | Credenciales distintas: ajusta `DB_USER` / `DB_PASSWORD`. |
| `Unknown database 'bitacora_dsy1105_dev'` | Falta crear la base en phpMyAdmin (paso 2). |
| `Communications link failure` | MariaDB detenido o puerto incorrecto: revisa el panel de XAMPP y `DB_PORT`. |
| Puerto 3306 ocupado por otro servicio | Usa ese servidor con `Iniciar-MySQL.bat` (si es MySQL) o configura el puerto de XAMPP y `DB_PORT`. |