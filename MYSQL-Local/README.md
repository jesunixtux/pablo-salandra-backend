# MySQL local

Entorno local para el backend Pablo Salandra. MySQL guarda sus archivos en
`data/mysql`; los binarios de MySQL, PHP y phpMyAdmin están en `runtime/`.
Esos directorios se excluyen de Git.

## Iniciar MySQL y phpMyAdmin

En Finder, dentro de `MYSQL-Local`, haz doble clic en `Iniciar.command`. También
puedes hacerlo desde una terminal:

```bash
./MYSQL-Local/start-local.sh
```

Esto inicializa MySQL la primera vez, lo liga solo a `127.0.0.1:3306`, crea la
base `bitacora` e inicia phpMyAdmin. macOS solicitará la contraseña del equipo
para iniciar su Apache local en el puerto 80 y habilitar la dirección corta:

Abre <http://localhost/phpmyadmin> y entra con el usuario `root` sin contraseña.
En esta instalación de desarrollo, el acceso sin contraseña está limitado al
Mac local; Apache restringe esta ruta a conexiones de loopback.

## Detener

Haz doble clic en `Detener.command`, o ejecuta `./MYSQL-Local/stop-local.sh`
desde una terminal.

También se pueden iniciar o detener por separado con `start-mysql.sh`,
`stop-mysql.sh` y `start-phpmyadmin.sh`.

El servidor usa MySQL Community Server 8.4.11 para macOS Apple Silicon,
PHP 8.3.36 y phpMyAdmin 5.2.3. MySQL viene del [archivo oficial de MySQL 8.4](https://dev.mysql.com/downloads/mysql/8.4.html);
PHP y phpMyAdmin vienen del [paquete oficial MAMP 7.5 para Apple Silicon](https://www.mamp.info/en/downloads/), extraído aquí sin instalar MAMP ni usar Homebrew.
Spring Boot está configurado para conectarse a
`jdbc:mysql://127.0.0.1:3306/bitacora` con `root` sin contraseña. Esa URL y las
credenciales se cambian en `src/main/resources/application.properties` del
backend. JPA crea las tablas `notebooks` y `student_entries` al iniciar la API.
