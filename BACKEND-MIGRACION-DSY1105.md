# Migración del backend Bitácora DSY1105

## Estado inicial y alcance

El backend original tenía dos tablas (`notebooks` y `student_entries`), CRUD sin autenticación, filtrado por correo proporcionado por el cliente y `ddl-auto=update`. No representaba asignaturas/periodos, publicación/cierre, cuentas, evidencias ni sesiones. Se amplió el mismo proyecto en `main`; Android se consultó como referencia y no se modificó. El caso académico y el catálogo actual de Android sitúan el MVP en 5°–8° Básico.

## Esquema y migraciones

Flyway tiene `V1__legacy_schema.sql` para instalaciones vacías y `V2__mvp_backend.sql` para añadir el modelo. En una base antigua no vacía se registra una línea base en versión 1 y se aplica V2. La migración no elimina tablas ni filas. `ddl-auto=validate` evita cambios implícitos de Hibernate.

- `users`: correo único, nombre, perfil, curso, hash BCrypt y estado activo.
- `teacher_assignments`: docente, curso y asignatura permitidos, sin duplicados.
- `user_sessions`: hash SHA-256 del token opaco, caducidad y revocación.
- `notebooks`: docente, curso, asignatura, mes/año, estudiantes, publicación, cierre, recordatorios y `updated_at`; índice único para el periodo de un docente.
- `notebook_students`: relación bitácora–estudiante cuando hay asignación explícita.
- `student_entries`: vínculo con bitácora y propietario, envío, revisión, comentario, docente revisor y `updated_at`.
- `evidences`: vínculo con registro y propietario, tipo, archivo/URL, metadatos y clave de almacenamiento privada.
- `idempotency_records`: clave y huella de solicitud por usuario/operación para recuperar el ID tras una respuesta perdida.

Antes de aplicar V2 a la base local se inspeccionaron las dos tablas y se generó un respaldo SQL fuera del repositorio. Había 2 bitácoras y 3 registros, sin registros huérfanos. Después de la migración MySQL 8.4 confirmó V1 como línea base y V2 aplicada, conservando 2 y 3 filas. Los registros antiguos conservan su contenido; como no tenían periodo ni asignatura, reciben mes 1, año 2026 y `Lenguaje y Comunicación` por defecto. Las bitácoras antiguas quedan publicadas desde su fecha de creación. Los IDs de cuenta de filas antiguas permanecen nulos y el acceso se resuelve por el correo histórico correspondiente cuando se crea una cuenta con ese correo.

## Autenticación y permisos

`POST /api/auth/register` solo crea `STUDENT` aunque el cliente envíe un campo `role`. `POST /api/auth/login` verifica BCrypt y devuelve `{token, expiresAt, user}`. `POST /api/auth/logout` revoca la sesión y `GET /api/auth/me` devuelve `{id, name, email, role, course, active, assignments}`. Los tokens aleatorios duran 12 horas; se guarda únicamente su hash. No hay docente predeterminado: se carga una cuenta ficticia por variables `BOOTSTRAP_TEACHER_*` al iniciar, con curso y asignatura válidos.

Los estudiantes ven bitácoras publicadas de su curso y, si hay lista de asignados, solo si figuran en ella. La respuesta muestra únicamente su correo en `assignedStudentEmails` para no revelar otros alumnos. Ven sus propios registros y evidencias. Los docentes crean, publican, cierran y revisan únicamente bitácoras de sus asignaciones y autoría; no ven borradores de estudiantes. Todas estas comprobaciones se hacen en servicios, usando la sesión, no correos suministrados en la petición. Las rutas restantes requieren Bearer. Un acceso ajeno responde `404` para no revelar IDs; un perfil sin permiso responde `403`.

## Contrato REST

### Bitácoras

`GET /api/notebooks` y `GET /api/notebooks/{id}` devuelven solo las visibles. `POST /api/notebooks` requiere `title`, `purpose`, `instructions`, `course`, `subject`, `month` (1–12), `year` y `Idempotency-Key`; admite `questions` y `assignedStudentEmails` como correos separados por coma. Una lista vacía habilita a todos los estudiantes del curso. `PUT /api/notebooks/{id}` actualiza solo antes de publicar. `POST /{id}/publish`, `POST /{id}/close`, `PUT /{id}/reminders` con `{ "enabled": true }` y `DELETE /{id}` requieren `If-Match: <updatedAt>`. El borrado solo aplica a una bitácora sin publicar y sin registros. `GET /{id}/participation` devuelve totales y correos de estudiantes sin envío al docente responsable. El historial cerrado sigue visible.

### Registros

`GET /api/student-entries` admite filtros `studentEmail`, `teacherEmail`, `classDate` y `status` (`DRAFT`, `SUBMITTED`, `PENDING`, `REVIEWED`) dentro del permiso del usuario. `POST /api/student-entries` recibe `notebookId`, `classDate` (`dd/MM/yyyy`), `content`, `activity`, `learning`, `doubts`, `isDraft` y `Idempotency-Key`. El servidor fija estudiante, docente y estados de revisión. Para envío, fecha, contenido, actividad y aprendizaje son obligatorios. `PUT /{id}` edita un borrador propio y puede enviarlo con `isDraft=false`; `POST /{id}/submit` envía un borrador completo. `PUT /{id}/review` recibe `{ "feedback": "..." }` del docente responsable. `DELETE /{id}` solo borra un borrador propio sin evidencias. Las escrituras con un ID existente requieren `If-Match`.

### Evidencias

`POST /api/evidences` es multipart con `entryRemoteId`, `kind` (`PHOTO`, `FILE` o `LINK`) e `Idempotency-Key`. Para archivos se envía la parte `file`; para enlaces se envía `url` HTTP/HTTPS. Se validan MIME y firma/contenido básico, 10 MiB máximo por archivo. Los archivos se guardan con nombre aleatorio fuera de rutas públicas y la base conserva solo metadatos. `GET /api/evidences/{id}` entrega bytes como adjunto o metadatos del enlace a su propietario y, tras el envío, al docente responsable. `content://` se rechaza como URL compartible. El `remoteRef` del DTO es el ID del servidor; no es una ruta local.

## Compatibilidad y conflictos

Las respuestas de bitácoras, registros y evidencias incluyen los campos de `NotebookDto`, `StudentEntryDto` y `EvidenceDto` preparados en Android: IDs remotos, estados, fechas en milisegundos y metadatos de revisión. `isDraft`, `isReviewed` e `isClosed` se serializan con esos nombres. Las peticiones legacy que incluyen correo/nombre de docente, estudiante o estado revisado no pueden imponer identidad ni permisos al servidor.

Para `POST`, una clave de idempotencia se almacena por usuario y operación junto a la huella de la solicitud. Repetirla con el mismo cuerpo devuelve el recurso existente; cambiar el cuerpo devuelve `409`. Para `PUT` y transiciones, el servidor exige `If-Match` con `updatedAt`; si cambió devuelve `412`, sin sobrescribir el registro. Las filas se bloquean durante las escrituras para serializar cambios concurrentes. El cliente debe volver a leer y pedir resolución del conflicto antes de reenviar cambios locales; no hay combinación automática.

La identidad `user` del login no es aún un `UserAccount` de Android: aquel modelo local tiene campos de RUT, género y partes del nombre que este backend académico no solicita. El futuro adaptador remoto deberá mapear la identidad del servidor sin inventar esos datos. Android sigue con `LocalAuthProvider`, `REMOTE_SYNC_ENABLED` desactivado y URL `10.0.2.2:8080`; el backend usa `8090`. No se realizó una prueba real app–backend ni se afirma que la app esté ya conectada.

## Validación y ejecución

- `./gradlew clean test`: pruebas aisladas H2, flujo de registro/login, permisos, bitácora mensual, asignación, duplicados, borrador, envío, revisión, evidencias, enlaces, reintentos y cierre.
- `TEST_DB_URL=... ./gradlew clean test`: mismo flujo en una base MySQL 8.4 de prueba independiente, con V1 y V2 aplicadas y relaciones verificadas.
- `./gradlew bootJar`: compilación del ejecutable.
- `./gradlew bootRun` con `bitacora`: Flyway V2 y arranque con validación JPA; `GET /api/notebooks` sin sesión respondió `401`; filas originales conservadas.

Para iniciar localmente:

```bash
./MYSQL-Local/start-mysql.sh
./gradlew bootRun
```

Para cambiar MySQL usa `DB_URL`, `DB_USER` y `DB_PASSWORD`. El servicio queda en `127.0.0.1:8090` salvo que cambies `SERVER_ADDRESS` y `SERVER_PORT`; `EVIDENCE_DIR` cambia la carpeta privada. El entorno local usa `root` sin contraseña como valor inicial de pruebas; fuera de él crea una cuenta MySQL de aplicación con permisos limitados. Para el emulador se usaría `http://10.0.2.2:8090/`. Para un teléfono físico, limita el acceso a una red confiable y configura explícitamente la interfaz del Mac y la URL Android; no publiques el servicio HTTP con Bearer en Internet.

## Pendiente para activar Android

Crear el proveedor de autenticación remota y el mapeo de `user` a la identidad visual Android; guardar y adjuntar el token real; cambiar la URL a `:8090`; implementar subida multipart y sincronización con `Idempotency-Key`/`If-Match`; resolver la migración de cuentas y datos Room existentes; probar en emulador y teléfono. Estas acciones pertenecen al repositorio Android y no forman parte de este cambio.
