package cl.duoc.pablo_salandra_backend;

import cl.duoc.pablo_salandra_backend.auth.TeacherAssignmentEntity;
import cl.duoc.pablo_salandra_backend.auth.TeacherAssignmentRepository;
import cl.duoc.pablo_salandra_backend.auth.UserEntity;
import cl.duoc.pablo_salandra_backend.auth.UserRepository;
import cl.duoc.pablo_salandra_backend.devdata.DemoDataLoader;
import cl.duoc.pablo_salandra_backend.studententry.StudentEntryEntity;
import cl.duoc.pablo_salandra_backend.studententry.StudentEntryRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:bitacora_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${TEST_DB_USER:sa}",
    "spring.datasource.password=${TEST_DB_PASSWORD:}",
    "spring.flyway.baseline-on-migrate=false",
    "app.evidence-dir=build/test-evidences"
})
@AutoConfigureMockMvc
class PabloSalandraBackendApplicationTests {
    private static final String COURSE = "5° Básico A";
    private static final String SUBJECT = "Matemática";
    private static final String PASSWORD = "SyntheticPassword123!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired TeacherAssignmentRepository assignments;
    @Autowired StudentEntryRepository studentEntries;
    @Autowired PasswordEncoder passwords;
    @Autowired ApplicationContext context;

    @Test
    void authenticationAndAuthorization() throws Exception {
        assertTrue(context.getBeansOfType(DemoDataLoader.class).isEmpty());
        assertEquals(401, mvc.perform(get("/api/notebooks")).andReturn().getResponse().getStatus());
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String teacherEmail = "teacher-" + suffix + "@example.test";
        String studentEmail = "student-" + suffix + "@example.test";
        UserEntity teacher = users.save(new UserEntity("Docente Ficticio", teacherEmail,
            passwords.encode(PASSWORD), "TEACHER", null));
        assignments.save(new TeacherAssignmentEntity(teacher.getId(), COURSE, SUBJECT));
        String teacherToken = login(teacherEmail, PASSWORD);

        assertEquals(401, mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(payload(Map.of("email", teacherEmail, "password", "wrong"))))
            .andReturn().getResponse().getStatus());

        String studentToken = register(studentEmail, COURSE);
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(studentToken)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("STUDENT"));
        mvc.perform(post("/api/notebooks").header("Authorization", bearer(studentToken))
            .header("Idempotency-Key", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
            .content(notebookInput()))
            .andExpect(status().isForbidden());

        mvc.perform(post("/api/auth/logout").header("Authorization", bearer(studentToken)))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(studentToken)))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(teacherToken)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.assignments[0].subject").value(SUBJECT));
    }

    @Test
    void monthlyNotebookEntriesEvidenceAndReview() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String teacherEmail = "teacher-" + suffix + "@example.test";
        String studentEmail = "student-" + suffix + "@example.test";
        String otherEmail = "other-" + suffix + "@example.test";
        String peerEmail = "peer-" + suffix + "@example.test";
        UserEntity teacher = users.save(new UserEntity("Docente Ficticio", teacherEmail,
            passwords.encode(PASSWORD), "TEACHER", null));
        assignments.save(new TeacherAssignmentEntity(teacher.getId(), COURSE, SUBJECT));
        String teacherToken = login(teacherEmail, PASSWORD);
        String studentToken = register(studentEmail, COURSE);
        String otherToken = register(otherEmail, "6° Básico A");
        register(peerEmail, COURSE);
        String otherTeacherEmail = "other-teacher-" + suffix + "@example.test";
        UserEntity otherTeacher = users.save(new UserEntity("Otra Docente Ficticia", otherTeacherEmail,
            passwords.encode(PASSWORD), "TEACHER", null));
        assignments.save(new TeacherAssignmentEntity(otherTeacher.getId(), COURSE, SUBJECT));
        String otherTeacherToken = login(otherTeacherEmail, PASSWORD);

        String key = UUID.randomUUID().toString();
        String assignedStudents = studentEmail + "," + peerEmail;
        JsonNode notebook = read(mvc.perform(post("/api/notebooks")
            .header("Authorization", bearer(teacherToken)).header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON).content(notebookInput(assignedStudents)))
            .andExpect(status().isCreated()).andReturn());
        long notebookId = notebook.get("id").asLong();
        assertEquals(SUBJECT, notebook.get("subject").asText());
        assertTrue(notebook.get("publishedAt").isNull());

        JsonNode replay = read(mvc.perform(post("/api/notebooks")
            .header("Authorization", bearer(teacherToken)).header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON).content(notebookInput(assignedStudents)))
            .andExpect(status().isCreated()).andReturn());
        assertEquals(notebookId, replay.get("id").asLong());
        mvc.perform(post("/api/notebooks").header("Authorization", bearer(teacherToken))
            .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
            .content(payload(Map.of("title", "Otro mes", "purpose", "Aprender",
                "instructions", "Escribir", "course", COURSE, "subject", SUBJECT,
                "month", 12, "year", 2026))))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/notebooks").header("Authorization", bearer(teacherToken))
            .header("Idempotency-Key", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
            .content(payload(Map.of("title", "Mes inválido", "purpose", "Aprender",
                "instructions", "Escribir", "course", COURSE, "subject", SUBJECT,
                "month", 13, "year", 2026))))
            .andExpect(status().isBadRequest());

        mvc.perform(post("/api/notebooks").header("Authorization", bearer(teacherToken))
            .header("Idempotency-Key", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
            .content(notebookInput(assignedStudents))).andExpect(status().isConflict());
        mvc.perform(get("/api/notebooks/" + notebookId).header("Authorization", bearer(studentToken)))
            .andExpect(status().isNotFound());

        notebook = read(mvc.perform(post("/api/notebooks/" + notebookId + "/publish")
            .header("Authorization", bearer(teacherToken))
            .header("If-Match", notebook.get("updatedAt").asText()))
            .andExpect(status().isOk()).andReturn());
        mvc.perform(post("/api/notebooks/" + notebookId + "/close")
            .header("Authorization", bearer(teacherToken)).header("If-Match", "0"))
            .andExpect(status().isPreconditionFailed());
        mvc.perform(get("/api/notebooks/" + notebookId).header("Authorization", bearer(studentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignedStudentEmails").value(studentEmail));
        mvc.perform(get("/api/notebooks/" + notebookId).header("Authorization", bearer(otherToken)))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/notebooks/" + notebookId).header("Authorization", bearer(otherTeacherToken)))
            .andExpect(status().isNotFound());

        String entryKey = UUID.randomUUID().toString();
        mvc.perform(post("/api/student-entries")
            .header("Authorization", bearer(studentToken)).header("Idempotency-Key", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload(Map.of("notebookId", notebookId, "classDate", "31/02/2026",
                "content", "Contenido", "activity", "Actividad", "learning", "Aprendizaje",
                "isDraft", false))))
            .andExpect(status().isBadRequest());
        JsonNode entry = read(mvc.perform(post("/api/student-entries")
            .header("Authorization", bearer(studentToken)).header("Idempotency-Key", entryKey)
            .contentType(MediaType.APPLICATION_JSON).content(entryInput(notebookId, true)))
            .andExpect(status().isCreated()).andReturn());
        long entryId = entry.get("id").asLong();
        mvc.perform(get("/api/student-entries/" + entryId).header("Authorization", bearer(otherToken)))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/student-entries/" + entryId).header("Authorization", bearer(teacherToken)))
            .andExpect(status().isNotFound());

        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain",
            "synthetic evidence".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String evidenceKey = UUID.randomUUID().toString();
        JsonNode evidence = read(mvc.perform(multipart("/api/evidences").file(file)
            .param("entryRemoteId", String.valueOf(entryId)).param("kind", "FILE")
            .header("Authorization", bearer(studentToken))
            .header("Idempotency-Key", evidenceKey))
            .andExpect(status().isCreated()).andReturn());
        long evidenceId = evidence.get("id").asLong();
        mvc.perform(get("/api/evidences/" + evidenceId).header("Authorization", bearer(otherToken)))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/evidences/" + evidenceId).header("Authorization", bearer(studentToken)))
            .andExpect(status().isOk())
            .andExpect(content().bytes("synthetic evidence".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        mvc.perform(multipart("/api/evidences")
            .param("entryRemoteId", String.valueOf(entryId)).param("kind", "LINK")
            .param("url", "content://local/file")
            .header("Authorization", bearer(studentToken))
            .header("Idempotency-Key", UUID.randomUUID().toString()))
            .andExpect(status().isBadRequest());
        JsonNode link = read(mvc.perform(multipart("/api/evidences")
            .param("entryRemoteId", String.valueOf(entryId)).param("kind", "LINK")
            .param("url", "https://example.test/ficticio")
            .header("Authorization", bearer(studentToken))
            .header("Idempotency-Key", UUID.randomUUID().toString()))
            .andExpect(status().isCreated()).andReturn());
        assertEquals("NOT_APPLICABLE", link.get("transferState").asText());

        entry = read(mvc.perform(put("/api/student-entries/" + entryId)
            .header("Authorization", bearer(studentToken)).header("If-Match", entry.get("updatedAt").asText())
            .contentType(MediaType.APPLICATION_JSON).content(entryInput(notebookId, false)))
            .andExpect(status().isOk()).andReturn());
        assertFalse(entry.get("isDraft").asBoolean());
        assertTrue(entry.get("evidences").isArray());
        assertEquals(2, entry.get("evidences").size());
        assertEquals(evidenceId, read(mvc.perform(multipart("/api/evidences").file(file)
            .param("entryRemoteId", String.valueOf(entryId)).param("kind", "FILE")
            .header("Authorization", bearer(studentToken)).header("Idempotency-Key", evidenceKey))
            .andExpect(status().isCreated()).andReturn()).get("id").asLong());
        mvc.perform(put("/api/student-entries/" + entryId).header("Authorization", bearer(studentToken))
            .header("If-Match", entry.get("updatedAt").asText()).contentType(MediaType.APPLICATION_JSON)
            .content(entryInput(notebookId, false))).andExpect(status().isConflict());

        JsonNode reviewed = read(mvc.perform(put("/api/student-entries/" + entryId + "/review")
            .header("Authorization", bearer(teacherToken)).header("If-Match", entry.get("updatedAt").asText())
            .contentType(MediaType.APPLICATION_JSON).content(payload(Map.of("feedback", "Bien hecho"))))
            .andExpect(status().isOk()).andReturn());
        assertTrue(reviewed.get("isReviewed").asBoolean());
        assertEquals(teacherEmail, reviewed.get("reviewerEmail").asText());
        mvc.perform(put("/api/student-entries/" + entryId + "/review")
            .header("Authorization", bearer(otherTeacherToken))
            .header("If-Match", reviewed.get("updatedAt").asText())
            .contentType(MediaType.APPLICATION_JSON).content(payload(Map.of("feedback", "Incorrecto"))))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/evidences/" + evidenceId).header("Authorization", bearer(teacherToken)))
            .andExpect(status().isOk());

        mvc.perform(get("/api/notebooks/" + notebookId + "/participation")
            .header("Authorization", bearer(teacherToken)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.participatingStudents").value(1))
            .andExpect(jsonPath("$.missingStudents").value(1))
            .andExpect(jsonPath("$.missingStudentEmails[0]").value(peerEmail));
        mvc.perform(get("/api/notebooks/" + notebookId + "/participation")
            .header("Authorization", bearer(studentToken))).andExpect(status().isForbidden());

        notebook = read(mvc.perform(post("/api/notebooks/" + notebookId + "/close")
            .header("Authorization", bearer(teacherToken))
            .header("If-Match", notebook.get("updatedAt").asText()))
            .andExpect(status().isOk()).andReturn());
        assertTrue(notebook.get("isClosed").asBoolean());
        mvc.perform(post("/api/student-entries").header("Authorization", bearer(studentToken))
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON).content(entryInput(notebookId, false)))
            .andExpect(status().isConflict());
        assertEquals(entryId, read(mvc.perform(post("/api/student-entries")
            .header("Authorization", bearer(studentToken)).header("Idempotency-Key", entryKey)
            .contentType(MediaType.APPLICATION_JSON).content(entryInput(notebookId, true)))
            .andExpect(status().isCreated()).andReturn()).get("id").asLong());
        assignments.deleteAll(assignments.findAll().stream()
            .filter(assignment -> assignment.getTeacherId().equals(teacher.getId())).toList());
        mvc.perform(get("/api/notebooks/" + notebookId).header("Authorization", bearer(teacherToken)))
            .andExpect(status().isNotFound());
        mvc.perform(put("/api/student-entries/" + entryId + "/review")
            .header("Authorization", bearer(teacherToken))
            .header("If-Match", reviewed.get("updatedAt").asText())
            .contentType(MediaType.APPLICATION_JSON).content(payload(Map.of("feedback", "Sin permiso"))))
            .andExpect(status().isNotFound());
    }

    @Test
    void periodValidationAndLegacyRowProtection() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String teacherEmail = "teacher-" + suffix + "@example.test";
        String legacyEmail = "legacy-" + suffix + "@example.test";
        UserEntity teacher = users.save(new UserEntity("Docente Ficticio", teacherEmail,
            passwords.encode(PASSWORD), "TEACHER", null));
        assignments.save(new TeacherAssignmentEntity(teacher.getId(), COURSE, SUBJECT));
        String teacherToken = login(teacherEmail, PASSWORD);

        // Filas legacy insertadas directamente: sin student_id y una huérfana sin notebook_id.
        StudentEntryEntity legacyDraft = studentEntries.save(new StudentEntryEntity(
            null, "Bitácora antigua", teacherEmail, legacyEmail, "Estudiante Antiguo",
            "05/03/2024", "Contenido antiguo", "Actividad", "Aprendizaje", "", true,
            System.currentTimeMillis(), false, ""));
        StudentEntryEntity legacySubmitted = studentEntries.save(new StudentEntryEntity(
            null, "Bitácora antigua", teacherEmail, legacyEmail, "Estudiante Antiguo",
            "06/03/2024", "Contenido antiguo", "Actividad", "Aprendizaje", "", false,
            System.currentTimeMillis(), false, ""));

        // Una cuenta nueva registrada con el correo legacy NO hereda esas filas.
        String claimerToken = register(legacyEmail, COURSE);
        JsonNode listed = read(mvc.perform(get("/api/student-entries")
            .header("Authorization", bearer(claimerToken)))
            .andExpect(status().isOk()).andReturn());
        for (JsonNode node : listed) {
            assertNotEquals(legacyDraft.getId().longValue(), node.get("id").asLong());
            assertNotEquals(legacySubmitted.getId().longValue(), node.get("id").asLong());
        }
        mvc.perform(get("/api/student-entries/" + legacyDraft.getId())
            .header("Authorization", bearer(claimerToken))).andExpect(status().isNotFound());
        mvc.perform(put("/api/student-entries/" + legacyDraft.getId())
            .header("Authorization", bearer(claimerToken)).header("If-Match", "1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload(Map.of("notebookId", 1, "classDate", "05/03/2024",
                "content", "x", "activity", "y", "learning", "z", "isDraft", true))))
            .andExpect(status().isNotFound());

        // El docente lista sin error 500 aunque existan filas huérfanas (notebook_id NULL).
        mvc.perform(get("/api/student-entries").header("Authorization", bearer(teacherToken)))
            .andExpect(status().isOk());
        mvc.perform(put("/api/student-entries/" + legacySubmitted.getId() + "/review")
            .header("Authorization", bearer(teacherToken)).header("If-Match", "1")
            .contentType(MediaType.APPLICATION_JSON).content(payload(Map.of("feedback", "ok"))))
            .andExpect(status().isNotFound());

        // La fecha del registro debe caer dentro del mes y año de la bitácora.
        String studentEmail = "student-" + suffix + "@example.test";
        String studentToken = register(studentEmail, COURSE);
        JsonNode notebook = read(mvc.perform(post("/api/notebooks")
            .header("Authorization", bearer(teacherToken)).header("Idempotency-Key", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON).content(notebookInput()))
            .andExpect(status().isCreated()).andReturn());
        long notebookId = notebook.get("id").asLong();
        mvc.perform(post("/api/notebooks/" + notebookId + "/publish")
            .header("Authorization", bearer(teacherToken))
            .header("If-Match", notebook.get("updatedAt").asText()))
            .andExpect(status().isOk());
        mvc.perform(post("/api/student-entries")
            .header("Authorization", bearer(studentToken)).header("Idempotency-Key", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload(Map.of("notebookId", notebookId, "classDate", "12/12/2026",
                "content", "Contenido", "activity", "Actividad", "learning", "Aprendizaje",
                "isDraft", false))))
            .andExpect(status().isBadRequest());
        // Como borrador sí se acepta (la validación de periodo aplica al enviar).
        JsonNode draft = read(mvc.perform(post("/api/student-entries")
            .header("Authorization", bearer(studentToken)).header("Idempotency-Key", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload(Map.of("notebookId", notebookId, "classDate", "12/12/2026",
                "content", "Contenido", "activity", "Actividad", "learning", "Aprendizaje",
                "isDraft", true))))
            .andExpect(status().isCreated()).andReturn());
        // Enviar ese borrador con fecha fuera de periodo también se rechaza.
        mvc.perform(post("/api/student-entries/" + draft.get("id").asLong() + "/submit")
            .header("Authorization", bearer(studentToken))
            .header("If-Match", draft.get("updatedAt").asText()))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/student-entries/" + draft.get("id").asLong())
            .header("Authorization", bearer(studentToken))
            .header("If-Match", draft.get("updatedAt").asText())
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload(Map.of("notebookId", notebookId, "classDate", "12/12/2026",
                "content", "Contenido", "activity", "Actividad", "learning", "Aprendizaje",
                "isDraft", false))))
            .andExpect(status().isBadRequest());
        // Con fecha dentro del periodo se envía correctamente.
        mvc.perform(put("/api/student-entries/" + draft.get("id").asLong())
            .header("Authorization", bearer(studentToken))
            .header("If-Match", draft.get("updatedAt").asText())
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload(Map.of("notebookId", notebookId, "classDate", "12/11/2026",
                "content", "Contenido", "activity", "Actividad", "learning", "Aprendizaje",
                "isDraft", false))))
            .andExpect(status().isOk());
    }

    private String notebookInput() throws Exception {
        return notebookInput("");
    }

    private String notebookInput(String assignedStudents) throws Exception {
        return payload(Map.of(
            "title", "Mes de prueba", "purpose", "Aprender", "instructions", "Escribir",
            "questions", "", "course", COURSE, "subject", SUBJECT, "month", 11, "year", 2026,
            "assignedStudentEmails", assignedStudents
        ));
    }

    private String entryInput(long notebookId, boolean draft) throws Exception {
        return payload(Map.of(
            "notebookId", notebookId, "classDate", "12/11/2026", "content", "Contenido",
            "activity", "Actividad", "learning", "Aprendizaje", "doubts", "", "isDraft", draft
        ));
    }

    private String register(String email, String course) throws Exception {
        JsonNode response = read(mvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload(Map.of("name", "Estudiante Ficticio", "email", email,
                "password", PASSWORD, "course", course, "role", "TEACHER"))))
            .andExpect(status().isCreated()).andReturn());
        assertEquals("STUDENT", response.get("user").get("role").asText());
        return response.get("token").asText();
    }

    private String login(String email, String password) throws Exception {
        return read(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(payload(Map.of("email", email, "password", password))))
            .andExpect(status().isOk()).andReturn()).get("token").asText();
    }

    private String payload(Object value) throws Exception { return json.writeValueAsString(value); }
    private String bearer(String token) { return "Bearer " + token; }
    private JsonNode read(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }
}
