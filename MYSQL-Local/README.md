# MySQL local

Entorno local para el backend Pablo Salandra. MySQL guarda sus archivos en
`data/mysql`; los binarios de MySQL, PHP y phpMyAdmin están en `runtime/`.
Esos directorios se excluyen de Git.

## Iniciar

En una terminal, desde esta carpeta:

```bash
./start-mysql.sh
```

Esto inicializa MySQL la primera vez, lo liga solo a `127.0.0.1:3306` y crea
la base vacía `bitacora`. Luego, en otra terminal, ejecuta:

```bash
./start-phpmyadmin.sh
```

Abre <http://127.0.0.1:8081> y entra con el usuario `root` sin contraseña.
En esta instalación de desarrollo, el acceso sin contraseña funciona solo
desde el Mac local. Antes de exponer el servidor a otros equipos, configura
una contraseña y una cuenta de aplicación.

Para detener MySQL:

```bash
./stop-mysql.sh
```

El servidor usa MySQL Community Server 8.4.11 para macOS Apple Silicon,
PHP 8.3.36 y phpMyAdmin 5.2.3. MySQL viene del [archivo oficial de MySQL 8.4](https://dev.mysql.com/downloads/mysql/8.4.html);
PHP y phpMyAdmin vienen del [paquete oficial MAMP 7.5 para Apple Silicon](https://www.mamp.info/en/downloads/), extraído aquí sin instalar MAMP ni usar Homebrew.
El backend Spring Boot podrá conectarse a
`jdbc:mysql://127.0.0.1:3306/bitacora` cuando configuremos sus credenciales.
