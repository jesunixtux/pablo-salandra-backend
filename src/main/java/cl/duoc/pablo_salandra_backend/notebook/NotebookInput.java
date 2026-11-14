package cl.duoc.pablo_salandra_backend.notebook;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotebookInput(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 10000) String purpose,
    @NotBlank @Size(max = 10000) String instructions,
    @Size(max = 10000) String questions,
    @NotBlank @Email @Size(max = 255) String teacherEmail,
    @NotBlank @Size(max = 255) String teacherName,
    @Size(max = 100) String course,
    @Size(max = 10000) String assignedStudentEmails
) {}
