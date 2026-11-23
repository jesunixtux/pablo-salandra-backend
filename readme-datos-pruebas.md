# Datos ficticios y pruebas DSY1105

Este entorno carga datos únicamente cuando Spring Boot se inicia con el perfil explícito `dev`. El perfil normal no contiene el cargador y continúa usando la base `bitacora`. El perfil `dev` usa por defecto la base independiente `bitacora_dsy1105_dev` y la carpeta privada `${user.home}/.local/share/pablo-salandra-backend/demo-evidences`.

`dev` se combina con cualquiera de los tres perfiles de motor, poniendo el motor al final: `dev,mysql`, `dev,xampp` (MariaDB de XAMPP) o `dev,standalone` (H2 en archivo `./data/bitacora`, sin servidor). En Windows hay scripts `Iniciar-MySQL.bat`, `Iniciar-XAMPP.bat` e `Iniciar-Standalone.bat` que ya usan esa combinación; ver [README-INSTALACION-WINDOWS-XAMPP.md](README-INSTALACION-WINDOWS-XAMPP.md).

## Requisitos e inicio

- JDK 21 y el Gradle Wrapper del proyecto.
- MySQL local iniciado con `./MYSQL-Local/start-mysql.sh` (o XAMPP con MariaDB, o nada en modo `standalone`).
- Nada más: si la base de desarrollo no existe, el backend la crea automáticamente al arrancar con `utf8mb4_general_ci`, Flyway crea las tablas y este perfil carga las cuentas.

Opcionalmente, para revisar la base creada:

```bash
./MYSQL-Local/runtime/mysql/bin/mysql --protocol=TCP --host=127.0.0.1 --port=3306 --user=root \
  -e 'SHOW DATABASES'
```

Iniciar Spring Boot y cargar los datos:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

El cargador se ejecuta al arrancar. Volver a iniciar el backend no duplica cuentas, asignaciones, bitácoras, registros ni evidencias. Tampoco modifica una fila ya existente: si la demo fue enviada, revisada, renombrada o cerrada por los usuarios, el reinicio la respeta y arranca igual. Solo falla si un correo de la demo pertenece a una cuenta real de otro rol o curso, caso en el que muestra un mensaje de conflicto.

Variables opcionales del perfil:

| Variable | Uso |
| --- | --- |
| `DEV_DB_URL` | URL de una base MySQL de desarrollo independiente |
| `DEV_DB_USER` / `DEV_DB_PASSWORD` | Credenciales de esa base de desarrollo |
| `DEMO_PASSWORD` | Contraseña común de las 30 cuentas ficticias |
| `DEMO_EVIDENCE_DIR` | Directorio aislado para los dos archivos demo |

La contraseña predeterminada es `DemoDSY1105!` y sirve exclusivamente para esta demostración local. Para definir otra sin guardarla en Git:

```bash
DEMO_PASSWORD='OtraClaveSoloDePrueba!' SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

Si las cuentas ya existen, el cargador no cambia sus hashes. Usa la misma `DEMO_PASSWORD` con la que se crearon o inicia una base de desarrollo nueva.

## Cuentas y datos esperados

El perfil crea 30 cuentas (6 docentes y 24 estudiantes, cuatro por curso). La lista completa de correos, roles, cursos y la contraseña exclusivamente local está en `cuentaa-de-ejemplos.txt`.

Todos los correos usan el dominio reservado `.test` y no pertenecen a personas reales.

| Correo | Rol | Curso o asignación |
| --- | --- | --- |
| `docente.matematica@demo.dsy1105.test` | TEACHER | 5° Básico A — Matemática |
| `docente.ciencias@demo.dsy1105.test` | TEACHER | 6° Básico A — Ciencias Naturales |
| `alba.5a@demo.dsy1105.test` | STUDENT | 5° Básico A |
| `benjamin.5a@demo.dsy1105.test` | STUDENT | 5° Básico A |
| `camila.6a@demo.dsy1105.test` | STUDENT | 6° Básico A |
| `diego.6a@demo.dsy1105.test` | STUDENT | 6° Básico A |

El catálogo completo aceptado por el backend es:

- Cursos: 5° Básico A, 5° Básico B, 6° Básico A, 6° Básico B, 7° Básico A y 8° Básico A.
- Asignaturas: Lenguaje y Comunicación, Matemática, Ciencias Naturales, Historia y Geografía e Inglés.

La carga crea estas bitácoras publicadas:

| Bitácora | Responsable | Estado |
| --- | --- | --- |
| Matemática - Noviembre 2026 | Docente Ada Demo | Abierta |
| Matemática - Octubre 2026 | Docente Ada Demo | Cerrada |
| Ciencias - Noviembre 2026 | Docente Bruno Demo | Abierta |

También crea tres registros: uno en borrador para Alba, uno enviado para Camila y uno revisado para Benjamín con retroalimentación. Sus evidencias cubren `PHOTO` (PNG), `FILE` (texto) y `LINK` (HTTPS). Los archivos viven en `DEMO_EVIDENCE_DIR`; la base guarda identificadores y metadatos.

## Pruebas automatizadas

Suite aislada con H2 en modo MySQL:

```bash
./gradlew clean test
./gradlew bootJar
```

Suite contra MySQL real de pruebas:

```bash
./MYSQL-Local/runtime/mysql/bin/mysql --protocol=TCP --host=127.0.0.1 --port=3306 --user=root \
  -e 'CREATE DATABASE IF NOT EXISTS bitacora_dsy1105_seed_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'

TEST_DB_URL='jdbc:mysql://127.0.0.1:3306/bitacora_dsy1105_seed_test?serverTimezone=America/Santiago' \
TEST_DB_USER=root TEST_DB_PASSWORD='' ./gradlew clean test
```

`TEST_DB_URL` debe apuntar siempre a una base de pruebas: la suite inserta filas ficticias. Las pruebas cubren registro/login, Bearer, roles, publicación y visibilidad, borradores, fecha dentro del periodo, envío, revisión, archivos y enlaces, cierre, accesos ajenos, idempotencia, `If-Match`, protección de filas históricas y ejecución repetida del cargador. `DemoDataLoaderTests` automatiza con MockMvc el recorrido estudiante–docente y puede ejecutarse sobre H2 o sobre la URL MySQL indicada.

## Recorrido de aceptación

Con MySQL y Spring Boot `dev` iniciados en `127.0.0.1:8090`, usa una terminal con `curl` y `jq`:

1. Inicia sesión como docente y consulta sus dos bitácoras.

```bash
TEACHER_TOKEN=$(curl -sS -X POST http://127.0.0.1:8090/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"docente.matematica@demo.dsy1105.test","password":"DemoDSY1105!"}' | jq -r .token)
curl -sS http://127.0.0.1:8090/api/notebooks -H "Authorization: Bearer $TEACHER_TOKEN" | jq
```

2. Inicia sesión como Alba y confirma que solo recibe Matemática de 5° Básico A.

```bash
STUDENT_TOKEN=$(curl -sS -X POST http://127.0.0.1:8090/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"alba.5a@demo.dsy1105.test","password":"DemoDSY1105!"}' | jq -r .token)
NOTEBOOK_ID=$(curl -sS http://127.0.0.1:8090/api/notebooks \
  -H "Authorization: Bearer $STUDENT_TOKEN" | jq -r '.[] | select(.title=="Matemática - Noviembre 2026") | .id')
```

3. Crea un borrador, adjunta un archivo y envíalo. Las claves de idempotencia del ejemplo son nuevas para esta ejecución.

```bash
ENTRY=$(curl -sS -X POST http://127.0.0.1:8090/api/student-entries \
  -H "Authorization: Bearer $STUDENT_TOKEN" -H "Idempotency-Key: acceptance-$(date +%s)" \
  -H 'Content-Type: application/json' \
  -d "{\"notebookId\":$NOTEBOOK_ID,\"classDate\":\"20/11/2026\",\"content\":\"Contenido de aceptación\",\"activity\":\"Actividad de aceptación\",\"learning\":\"Aprendizaje de aceptación\",\"doubts\":\"\",\"isDraft\":true}")
ENTRY_ID=$(printf '%s' "$ENTRY" | jq -r .id)
ENTRY_VERSION=$(printf '%s' "$ENTRY" | jq -r .updatedAt)
printf 'evidencia ficticia\n' > /tmp/dsy1105-evidencia-demo.txt
curl -sS -X POST http://127.0.0.1:8090/api/evidences \
  -H "Authorization: Bearer $STUDENT_TOKEN" -H "Idempotency-Key: evidence-$(date +%s)" \
  -F "entryRemoteId=$ENTRY_ID" -F 'kind=FILE' -F 'file=@/tmp/dsy1105-evidencia-demo.txt;type=text/plain' | jq
SUBMITTED=$(curl -sS -X POST "http://127.0.0.1:8090/api/student-entries/$ENTRY_ID/submit" \
  -H "Authorization: Bearer $STUDENT_TOKEN" -H "If-Match: $ENTRY_VERSION")
SUBMITTED_VERSION=$(printf '%s' "$SUBMITTED" | jq -r .updatedAt)
```

4. Revísalo como docente y consulta la retroalimentación como Alba.

```bash
curl -sS -X PUT "http://127.0.0.1:8090/api/student-entries/$ENTRY_ID/review" \
  -H "Authorization: Bearer $TEACHER_TOKEN" -H "If-Match: $SUBMITTED_VERSION" \
  -H 'Content-Type: application/json' -d '{"feedback":"Buen trabajo de aceptación"}' | jq
curl -sS "http://127.0.0.1:8090/api/student-entries/$ENTRY_ID" \
  -H "Authorization: Bearer $STUDENT_TOKEN" | jq
```

5. Comprueba que Benjamín no puede leer el registro de Alba; el resultado esperado es HTTP `404`.

```bash
OTHER_TOKEN=$(curl -sS -X POST http://127.0.0.1:8090/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"benjamin.5a@demo.dsy1105.test","password":"DemoDSY1105!"}' | jq -r .token)
curl -sS -o /dev/null -w '%{http_code}\n' "http://127.0.0.1:8090/api/student-entries/$ENTRY_ID" \
  -H "Authorization: Bearer $OTHER_TOKEN"
```

Endpoints utilizados: `/api/auth/login`, `/api/notebooks`, `/api/student-entries`, `/api/student-entries/{id}/submit`, `/api/student-entries/{id}/review`, `/api/evidences` y `/api/evidences/{id}`. Las mutaciones usan `Idempotency-Key` o `If-Match` según el contrato.

## Restaurar sin afectar otros datos

No hace falta borrar ni vaciar ninguna base. Detén el backend, crea otra base con un nombre nuevo y otra carpeta vacía, y vuelve a iniciar:

```bash
DEV_DB_URL='jdbc:mysql://127.0.0.1:3306/bitacora_dsy1105_dev_2?serverTimezone=America/Santiago' \
DEMO_EVIDENCE_DIR="$PWD/build/demo-evidences-2" \
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

Primero crea `bitacora_dsy1105_dev_2` con el mismo comando `CREATE DATABASE IF NOT EXISTS` mostrado arriba. La base y carpeta anteriores quedan intactas. No uses `bitacora` ni una base con información importante para este perfil.

## Problemas conocidos

- La API usa HTTP local; no transmitas estos tokens fuera de `127.0.0.1` o una red de desarrollo controlada.
- Las fechas demo corresponden a octubre y noviembre de 2026 para probar la validación mensual.
- La prueba automatizada verifica una segunda ejecución del cargador con los mismos IDs. El resultado esperado tras reiniciar es conservar 30 usuarios, 6 asignaciones, 3 bitácoras, 3 registros y 3 evidencias.
- El backend no incorpora un endpoint para borrar la demo. La restauración segura consiste en usar otra base de pruebas, sin operaciones destructivas.
