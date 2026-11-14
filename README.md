# Pablo Salandra Backend

API REST para bitácoras docentes y registros de clase. Guarda los datos en la
base MySQL local `bitacora`.

## Requisitos y ejecución

- JDK 21
- Gradle Wrapper incluido

Desde esta carpeta, inicia MySQL y luego Spring Boot:

```bash
./MYSQL-Local/start-mysql.sh
./gradlew bootRun
```

La API queda disponible en `http://localhost:8080`. En el emulador Android,
`10.0.2.2:8080` dirige al servidor del Mac. Esta API es la que usa MySQL;
el directorio `backend/` dentro del repositorio Android contiene otro servidor
con H2. Inicia solo uno de los dos en el puerto 8080.

La URL de MySQL, el usuario y la contraseña se cambian en
`src/main/resources/application.properties`. La configuración actual usa
`root` sin contraseña, tal como el entorno local. La API escucha solo en
`127.0.0.1`; para usar un dispositivo físico se debe ajustar esa dirección y
la URL del cliente Android.

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
  "teacherName": "Docente de prueba",
  "course": "1° Medio A",
  "assignedStudentEmails": "alumno@example.com"
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
  "isDraft": false,
  "isReviewed": false,
  "teacherFeedback": ""
}
```

Los campos obligatorios y el formato de fecha se validan al recibir solicitudes.
Una bitácora debe existir antes de crear un registro asociado. Para guardar un
borrador, `isDraft` debe ser `true` y al menos uno de `content`, `activity`,
`learning` o `doubts` debe tener contenido.

## Límite de esta versión

No incluye autenticación. Los filtros por correo son criterios de búsqueda,
no controles de acceso. La aplicación Android usa esta API para publicar y
asignar bitácoras, crear y editar registros, y registrar revisiones. Room
conserva una copia local para mostrar los datos ya sincronizados cuando el
servidor no está disponible; las operaciones de escritura requieren conexión.
