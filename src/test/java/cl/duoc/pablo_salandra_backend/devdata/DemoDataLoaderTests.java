package cl.duoc.pablo_salandra_backend.devdata;

import cl.duoc.pablo_salandra_backend.auth.UserEntity;
import cl.duoc.pablo_salandra_backend.auth.UserRepository;
import cl.duoc.pablo_salandra_backend.evidence.EvidenceRepository;
import cl.duoc.pablo_salandra_backend.notebook.NotebookEntity;
import cl.duoc.pablo_salandra_backend.notebook.NotebookRepository;
import cl.duoc.pablo_salandra_backend.studententry.StudentEntryEntity;
import cl.duoc.pablo_salandra_backend.studententry.StudentEntryRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:demo_data_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${TEST_DB_USER:sa}",
    "spring.datasource.password=${TEST_DB_PASSWORD:}",
    "spring.flyway.baseline-on-migrate=false",
    "app.demo.password=DemoIntegrationOnly!",
    "app.evidence-dir=build/test-demo-evidences"
})
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class DemoDataLoaderTests {
    @Autowired DemoDataLoader loader;
    @Autowired UserRepository users;
    @Autowired NotebookRepository notebooks;
    @Autowired StudentEntryRepository entries;
    @Autowired EvidenceRepository evidences;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void loadingTwicePreservesDemoRowsAndIdentifiers() {
        List<UserEntity> demoUsers = demoUsers();
        List<NotebookEntity> demoNotebooks = demoNotebooks(demoUsers);
        Set<Long> notebookIds = demoNotebooks.stream().map(NotebookEntity::getId).collect(java.util.stream.Collectors.toSet());
        var demoEntries = entries.findAll().stream().filter(entry -> notebookIds.contains(entry.getNotebookId())).toList();
        var demoEvidences = evidences.findAll().stream()
            .filter(evidence -> demoEntries.stream().anyMatch(entry -> entry.getId().equals(evidence.getEntryId()))).toList();
        Set<Long> userIds = demoUsers.stream().map(UserEntity::getId).collect(java.util.stream.Collectors.toSet());
        Set<Long> entryIds = demoEntries.stream().map(entry -> entry.getId()).collect(java.util.stream.Collectors.toSet());

        assertEquals(30, demoUsers.size());
        assertEquals(3, demoNotebooks.size());
        assertEquals(3, demoEntries.size());
        assertEquals(Set.of("PHOTO", "FILE", "LINK"),
            demoEvidences.stream().map(evidence -> evidence.getKind()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(1, demoNotebooks.stream().filter(NotebookEntity::isClosed).count());
        assertEquals(1, demoEntries.stream().filter(entry -> entry.isDraft()).count());
        assertEquals(1, demoEntries.stream().filter(entry -> !entry.isDraft() && !entry.isReviewed()).count());
        assertEquals(1, demoEntries.stream().filter(entry -> entry.isReviewed() &&
            !entry.getTeacherFeedback().isBlank()).count());

        loader.run(null);

        assertEquals(userIds, demoUsers().stream().map(UserEntity::getId).collect(java.util.stream.Collectors.toSet()));
        assertEquals(notebookIds, demoNotebooks(demoUsers()).stream().map(NotebookEntity::getId)
            .collect(java.util.stream.Collectors.toSet()));
        assertEquals(entryIds, entries.findAll().stream().filter(entry -> notebookIds.contains(entry.getNotebookId()))
            .map(entry -> entry.getId()).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    @Transactional
    void restartAfterDemoRecordsWereUsedKeepsExistingRows() {
        UserEntity teacher = users.findByEmailIgnoreCase(DemoDataLoader.TEACHER_MATH).orElseThrow();
        NotebookEntity mathOpen = notebooks.findByTeacherIdAndCourseAndSubjectAndMonthAndYear(
            teacher.getId(), "5° Básico A", "Matemática", 11, 2026).orElseThrow();
        UserEntity alba = users.findByEmailIgnoreCase(DemoDataLoader.STUDENT_ALBA).orElseThrow();
        StudentEntryEntity draft = entries.findByNotebookId(mathOpen.getId()).stream()
            .filter(entry -> alba.getId().equals(entry.getStudentId())).findFirst().orElseThrow();

        // La usuaria dejó la demo avanzada: envió su borrador y la docente cerró la bitácora.
        draft.submit();
        entries.save(draft);
        mathOpen.close();
        notebooks.save(mathOpen);

        loader.run(null);

        assertEquals(30, demoUsers().size());
        assertEquals(3, demoNotebooks(demoUsers()).size());
        assertTrue(notebooks.findById(mathOpen.getId()).orElseThrow().isClosed());
        assertFalse(entries.findById(draft.getId()).orElseThrow().isDraft());
    }

    @Test
    @Transactional
    void acceptanceFlowUsesSeededAccountsAndEnforcesOwnership() throws Exception {
        String teacherToken = login(DemoDataLoader.TEACHER_MATH);
        String studentToken = login(DemoDataLoader.STUDENT_ALBA);
        String otherStudentToken = login(DemoDataLoader.STUDENT_BENJAMIN);
        JsonNode teacherNotebooks = read(mvc.perform(get("/api/notebooks")
            .header("Authorization", bearer(teacherToken))).andExpect(status().isOk()).andReturn());
        assertEquals(2, teacherNotebooks.size());

        JsonNode studentNotebooks = read(mvc.perform(get("/api/notebooks")
            .header("Authorization", bearer(studentToken))).andExpect(status().isOk()).andReturn());
        int visibleDemoNotebooks = 0;
        for (JsonNode notebook : studentNotebooks) {
            assertEquals("5° Básico A", notebook.get("course").asText());
            if (notebook.get("title").asText().startsWith("Matemática - ")) visibleDemoNotebooks++;
        }
        assertEquals(2, visibleDemoNotebooks);
        JsonNode open = findOpen(studentNotebooks);
        long notebookId = open.get("id").asLong();
        String entryKey = UUID.randomUUID().toString();
        JsonNode draft = read(mvc.perform(post("/api/student-entries")
            .header("Authorization", bearer(studentToken)).header("Idempotency-Key", entryKey)
            .contentType(MediaType.APPLICATION_JSON).content(payload(Map.of(
                "notebookId", notebookId, "classDate", "20/11/2026", "content", "Contenido de aceptación",
                "activity", "Actividad de aceptación", "learning", "Aprendizaje de aceptación",
                "doubts", "", "isDraft", true))))
            .andExpect(status().isCreated()).andReturn());
        long entryId = draft.get("id").asLong();

        MockMultipartFile file = new MockMultipartFile("file", "aceptacion.txt", "text/plain",
            "evidencia de aceptación".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/evidences").file(file)
            .param("entryRemoteId", Long.toString(entryId)).param("kind", "FILE")
            .header("Authorization", bearer(studentToken)).header("Idempotency-Key", UUID.randomUUID()))
            .andExpect(status().isCreated());
        JsonNode submitted = read(mvc.perform(post("/api/student-entries/" + entryId + "/submit")
            .header("Authorization", bearer(studentToken)).header("If-Match", draft.get("updatedAt").asText()))
            .andExpect(status().isOk()).andReturn());
        JsonNode reviewed = read(mvc.perform(put("/api/student-entries/" + entryId + "/review")
            .header("Authorization", bearer(teacherToken)).header("If-Match", submitted.get("updatedAt").asText())
            .contentType(MediaType.APPLICATION_JSON).content(payload(Map.of("feedback", "Retroalimentación de aceptación"))))
            .andExpect(status().isOk()).andReturn());
        assertEquals("Retroalimentación de aceptación", reviewed.get("teacherFeedback").asText());
        mvc.perform(get("/api/student-entries/" + entryId).header("Authorization", bearer(studentToken)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.isReviewed").value(true))
            .andExpect(jsonPath("$.teacherFeedback").value("Retroalimentación de aceptación"));
        mvc.perform(get("/api/student-entries/" + entryId).header("Authorization", bearer(otherStudentToken)))
            .andExpect(status().isNotFound());

        JsonNode replay = read(mvc.perform(post("/api/student-entries")
            .header("Authorization", bearer(studentToken)).header("Idempotency-Key", entryKey)
            .contentType(MediaType.APPLICATION_JSON).content(payload(Map.of(
                "notebookId", notebookId, "classDate", "20/11/2026", "content", "Contenido de aceptación",
                "activity", "Actividad de aceptación", "learning", "Aprendizaje de aceptación",
                "doubts", "", "isDraft", true))))
            .andExpect(status().isCreated()).andReturn());
        assertEquals(entryId, replay.get("id").asLong());
    }

    private List<UserEntity> demoUsers() {
        return users.findAll().stream().filter(user -> user.getEmail().endsWith("@demo.dsy1105.test")).toList();
    }

    private List<NotebookEntity> demoNotebooks(List<UserEntity> demoUsers) {
        Set<Long> teachers = demoUsers.stream().filter(user -> "TEACHER".equals(user.getRole()))
            .map(UserEntity::getId).collect(java.util.stream.Collectors.toSet());
        return notebooks.findAll().stream().filter(notebook -> teachers.contains(notebook.getTeacherId())).toList();
    }

    private JsonNode findOpen(JsonNode notebooks) {
        for (JsonNode notebook : notebooks) {
            if ("Matemática - Noviembre 2026".equals(notebook.get("title").asText())) return notebook;
        }
        throw new AssertionError("Open demo notebook missing");
    }

    private String login(String email) throws Exception {
        return read(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(payload(Map.of("email", email, "password", "DemoIntegrationOnly!"))))
            .andExpect(status().isOk()).andReturn()).get("token").asText();
    }

    private String payload(Object value) throws Exception { return json.writeValueAsString(value); }
    private String bearer(String token) { return "Bearer " + token; }
    private JsonNode read(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }
}
