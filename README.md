# Bitácora DSY1105 — backend

API Spring Boot 4.1.2, Java 21 y base de datos compatible (MySQL, MariaDB de XAMPP o H2 en modo independiente) para bitácoras mensuales de 5° a 8° Básico. La API escucha en `127.0.0.1:8090` y usa la base `bitacora` en `127.0.0.1:3306` de forma predeterminada. La aplicación Android aún usa autenticación y almacenamiento locales; su sincronización remota está desactivada.

## Iniciar

```bash
./MYSQL-Local/start-mysql.sh
./gradlew bootRun
```

Flyway crea o amplía el esquema al arrancar. `spring.jpa.hibernate.ddl-auto=validate` comprueba las entidades. Antes de actualizar una base existente, haz un respaldo. Las migraciones se describen en [BACKEND-MIGRACION-DSY1105.md](BACKEND-MIGRACION-DSY1105.md).

### Perfiles de base de datos

El motor se elige con un perfil, sin tocar el código ni la app Android. No hay cambio
automático de motor si una conexión falla: se usa siempre el perfil indicado.

| Perfil | Motor | URL predeterminada |
| --- | --- | --- |
| `mysql` | MySQL tradicional | `jdbc:mysql://127.0.0.1:3306/bitacora` |
| `xampp` | MariaDB de XAMPP (conector `jdbc:mariadb:`) | `jdbc:mariadb://127.0.0.1:3306/bitacora` |
| `standalone` | H2 en archivo (`./data/bitacora.mv.db`), sin servidor | sin configuración |
| `dev` | Solo datos: 30 cuentas ficticias; se combina con los tres anteriores | respeta el perfil del motor |

```bash
SPRING_PROFILES_ACTIVE=dev,xampp DB_NAME=bitacora_dsy1105_dev ./gradlew bootRun
SPRING_PROFILES_ACTIVE=dev,standalone ./gradlew bootRun
```

En Windows existen `Iniciar-XAMPP.bat`, `Iniciar-MySQL.bat` e `Iniciar-Standalone.bat`
(doble clic, comprueban Java y el puerto, ofrecen iniciar el módulo MySQL de XAMPP si
está parado, e inician con `dev` más el perfil del motor);
ver [README-INSTALACION-WINDOWS-XAMPP.md](README-INSTALACION-WINDOWS-XAMPP.md). En
`mysql` y `xampp`, si la base configurada no existe se crea automáticamente con
`utf8mb4_general_ci` antes de migrar (el usuario necesita privilegio CREATE); en H2 no
hay servidor que configurar. El modo `standalone` es exclusivamente para
demostraciones académicas y desarrollo local.

Variables opcionales: `DB_URL` (URL completa), `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`, `SERVER_ADDRESS` y `EVIDENCE_DIR`. Por defecto, `DB_USER=root` y `DB_PASSWORD` vacío se usan únicamente para estas bases locales de pruebas. Para otro entorno usa un usuario de aplicación y contraseña propia. Los archivos se guardan fuera del repositorio en `${user.home}/.local/share/pablo-salandra-backend/evidences`, salvo que configures `EVIDENCE_DIR`.

El registro público crea solo estudiantes. Para cargar un docente ficticio con asignación de curso y asignatura, configura antes del arranque `BOOTSTRAP_TEACHER_EMAIL`, `BOOTSTRAP_TEACHER_PASSWORD`, `BOOTSTRAP_TEACHER_NAME`, `BOOTSTRAP_TEACHER_COURSE` y `BOOTSTRAP_TEACHER_SUBJECT`. La contraseña debe tener al menos 8 caracteres y se guarda con BCrypt; si la cuenta ya existe, el arranque solo agrega la asignación y no cambia su contraseña. No pongas estas variables en archivos versionados.

## API

`POST /api/auth/register` recibe `name`, `email`, `password`, `course`. `POST /api/auth/login` recibe `email`, `password`. Ambos devuelven `token`, `expiresAt` y `user`. Envía `Authorization: Bearer <token>` para el resto de la API. `GET /api/auth/me` devuelve la identidad; `POST /api/auth/logout` revoca la sesión. Los tokens expiran en 12 horas.

| Recurso | Operaciones |
| --- | --- |
| `/api/notebooks` | `GET`, `POST`; `GET/PUT/DELETE /{id}`; `POST /{id}/publish`, `POST /{id}/close`; `PUT /{id}/reminders`; `GET /{id}/participation` |
| `/api/student-entries` | `GET`, `POST`; `GET/PUT/DELETE /{id}`; `POST /{id}/submit`; `PUT /{id}/review` |
| `/api/evidences` | `POST` multipart; `GET /{id}` |

La creación de bitácoras y registros, y la subida de evidencias, requieren `Idempotency-Key` único por operación. Las modificaciones, publicación, cierre, recordatorios y revisión requieren `If-Match` con el valor `updatedAt` leído antes. Una versión obsoleta devuelve `412`; una clave reutilizada con otro contenido devuelve `409`. Consulta cuerpos, permisos y límites en [BACKEND-MIGRACION-DSY1105.md](BACKEND-MIGRACION-DSY1105.md).

## Pruebas

```bash
./gradlew clean test
./gradlew bootJar
```

Las pruebas usan H2 en modo MySQL por defecto. Para repetirlas en una base MySQL **de prueba independiente**, configura `TEST_DB_URL`, `TEST_DB_USER` y `TEST_DB_PASSWORD` y ejecuta `./gradlew clean test`. No apuntes `TEST_DB_URL` a `bitacora`: las pruebas insertan cuentas y datos ficticios.

En el emulador Android, el Mac es `10.0.2.2` y la API está en el puerto `8090`. La app todavía tiene URL local `:8080` e integración remota desactivada; cambiarla y activar el cliente corresponde a la siguiente etapa. Para un teléfono físico en una red confiable, configura `SERVER_ADDRESS` con la interfaz del Mac y usa su IP local en Android. No expongas esta API HTTP con tokens Bearer a Internet.
