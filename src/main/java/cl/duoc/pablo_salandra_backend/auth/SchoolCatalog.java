package cl.duoc.pablo_salandra_backend.auth;

import java.time.Year;
import java.util.Set;

public final class SchoolCatalog {
    private SchoolCatalog() {}

    public static final Set<String> COURSES = Set.of(
        "5° Básico A", "5° Básico B", "6° Básico A", "6° Básico B", "7° Básico A", "8° Básico A"
    );
    public static final Set<String> SUBJECTS = Set.of(
        "Lenguaje y Comunicación", "Matemática", "Ciencias Naturales",
        "Historia y Geografía", "Inglés"
    );

    public static boolean validPeriod(int month, int year) {
        return month >= 1 && month <= 12 && year >= 2020 && year <= Year.now().getValue() + 2;
    }
}
