# Instalación en Windows con XAMPP (Backend Bitácora DSY1105)

Guía para ejecutar el backend en computadores de la universidad que ya tienen XAMPP
(MariaDB). No se necesita instalar MySQL ni tocar la aplicación Android. La API
escucha siempre en `http://127.0.0.1:8090`.

## Requisitos

- **JDK 21** (`java -version` debe responder; si no, instálalo y reabre la consola).
- **XAMPP** con el módulo **MySQL (MariaDB)** iniciado desde su panel de control.
- El repositorio del backend descargado o clonado (la carpeta donde está `gradlew.bat`).

## Opción rápida: doble clic

Ejecuta `Iniciar-XAMPP.bat` desde la raíz del repositorio. El script comprueba Java
y el puerto 3306; si el módulo MySQL de XAMPP está parado, ofrece iniciarlo con el
propio `mysqld` de XAMPP (nunca arranca una segunda instancia si el puerto ya está
ocupado). Luego arranca Spring Boot con los perfiles `dev,xampp`. **No hay que crear
la base, las tablas ni las cuentas a mano**: el backend crea la base configurada con
cotejamiento `utf8mb4_general_ci` (compatible con MySQL y MariaDB), Flyway migrará y
el perfil `dev` cargará las 30 cuentas ficticias. Si falta Java, el Gradle Wrapper o
los permisos, el script muestra el error y queda en pantalla.

## Pasos manuales

### 1. Iniciar XAMPP (opcional)

1. Abre el **Panel de Control de XAMPP**.
2. En la fila **MySQL (MariaDB)** pulsa **Start**, o deja que `Iniciar-XAMPP.bat` lo
   haga. Debe quedar escuchando en el puerto **3306** (o el que configures).
3. Si el puerto está ocupado por otro MySQL, cambia el puerto de MariaDB en
   `my.ini` o usa el tuyo propio con `DB_PORT`.

### 2. La base de datos

No se crea a mano ni hace falta phpMyAdmin: al arrancar, el backend comprueba en el
servidor si existe `DB_NAME` (`bitacora_dsy1105_dev` en los scripts) y, si falta,
ejecuta `CREATE DATABASE ... CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci`
conectando sin seleccionar base. Flyway crea o amplía las tablas después. Nunca
borra ni modifica bases existentes; si la base ya tiene datos, las migraciones solo
se aplican hacia delante.

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

Primer arranque exitoso: se crea la base si falta (utf8mb4 / utf8mb4_general_ci),
Flyway aplica las migraciones V1/V2, Hibernate valida el esquema y se cargan las
30 cuentas ficticias (`cuentaa-de-ejemplos.txt`, contraseña común `DemoDSY1105!`).
Los reinicios siguientes no duplican ni sobrescriben datos, aunque hayas modificado,
enviado, revisado o cerrado los registros de demo.

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
| `No se pudo comprobar ni crear la base ...` | El usuario no tiene privilegios CREATE o faltan credenciales: revisa `DB_USER` / `DB_PASSWORD` (o crea la base a mano). |
| `Access denied for user 'root'` | Credenciales distintas: ajusta `DB_USER` / `DB_PASSWORD`. |
| `Communications link failure` | MariaDB detenido o puerto incorrecto: revisa el panel de XAMPP y `DB_PORT`. |
| Puerto 3306 ocupado por otro servicio | Usa ese servidor con `Iniciar-MySQL.bat` (si es MySQL) o configura el puerto de XAMPP y `DB_PORT`. |