# Pablo Salandra Backend

Primera versión de la API REST para bitácoras docentes y registros de clase.
Por ahora los datos se guardan en memoria y se pierden al reiniciar el servidor.

## Requisitos y ejecución

- JDK 21
- Gradle Wrapper incluido

Desde esta carpeta:

```bash
./gradlew bootRun
```

La API queda disponible en `http://localhost:8080`.

## Endpoints

### Bitácoras

Base: `/api/notebooks`

- `GET /api/notebooks`
- `GET /api/notebooks/{id}`
- `POST /api/notebooks`
- `PUT /api/notebooks/{id}`
- `DELETE /api/notebooks/{id}`

Cuerpo para crear o reemplazar:

```json
{
  "title": "Octubre",
  "purpose": "Reflexionar sobre lo aprendido",
  "instructions": "Describe la actividad de la clase",
  "questions": "¿Qué aprendiste?",
  "teacherEmail": "docente@example.com",
  "teacherName": "Docente de prueba"
}
```

### Registros de clase

Base: `/api/student-entries`

- `GET /api/student-entries`
- `GET /api/student-entries?studentEmail=alumno@example.com`
- `GET /api/student-entries?teacherEmail=docente@example.com`
- `GET /api/student-entries/{id}`
- `POST /api/student-entries`
- `PUT /api/student-entries/{id}`
- `DELETE /api/student-entries/{id}`

Cuerpo para crear o reemplazar:

```json
{
  "notebookId": 1,
  "studentEmail": "alumno@example.com",
  "studentName": "Estudiante de prueba",
  "classDate": "12/11/2026",
  "content": "Hoy aprendí...",
  "activity": "Resolvimos ejercicios",
  "learning": "Aprendí a...",
  "doubts": "",
  "isDraft": false
}
```

Los campos obligatorios y el formato de fecha se validan al recibir solicitudes.
Una bitácora debe existir antes de crear un registro asociado. Para guardar un
borrador, `isDraft` debe ser `true` y al menos uno de `content`, `activity`,
`learning` o `doubts` debe tener contenido.

## Límite de esta versión

No incluye autenticación. Los filtros por correo son criterios de búsqueda,
no controles de acceso. Los datos se guardan en MySQL local (`bitacora`); para
iniciar la API, primero levanta el entorno con `MYSQL-Local/start-local.sh`.
