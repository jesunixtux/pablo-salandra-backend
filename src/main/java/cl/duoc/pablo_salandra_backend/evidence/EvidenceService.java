package cl.duoc.pablo_salandra_backend.evidence;

import cl.duoc.pablo_salandra_backend.auth.CurrentUser;
import cl.duoc.pablo_salandra_backend.auth.UserEntity;
import cl.duoc.pablo_salandra_backend.studententry.StudentEntryEntity;
import cl.duoc.pablo_salandra_backend.studententry.StudentEntryService;
import cl.duoc.pablo_salandra_backend.sync.IdempotencyService;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EvidenceService {
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> MIMES = Set.of(
        "image/jpeg", "image/png", "image/webp", "application/pdf", "text/plain",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private final EvidenceRepository evidences;
    private final StudentEntryService entries;
    private final CurrentUser current;
    private final IdempotencyService idempotency;
    private final Path directory;

    public EvidenceService(EvidenceRepository evidences, StudentEntryService entries,
            CurrentUser current, IdempotencyService idempotency,
            @Value("${app.evidence-dir}") String directory) {
        this.evidences = evidences;
        this.entries = entries;
        this.current = current;
        this.idempotency = idempotency;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    @Transactional
    public EvidenceDto create(long entryId, String kind, MultipartFile file, String url, String key) {
        UserEntity student = current.require("STUDENT");
        if (!Set.of("PHOTO", "FILE", "LINK").contains(kind))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid evidence kind");
        boolean link = "LINK".equals(kind);
        byte[] bytes = new byte[0];
        String mime = null;
        String fileName = null;
        if (link) {
            if (file != null && !file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            if (url == null || url.length() > 2048 || !validLink(url))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HTTP(S) link required");
        } else {
            if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES ||
                    !MIMES.contains(file.getContentType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file or MIME type");
            }
            if ("PHOTO".equals(kind) && !file.getContentType().startsWith("image/"))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            mime = file.getContentType();
            fileName = safeName(file.getOriginalFilename());
            try { bytes = file.getBytes(); }
            catch (IOException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR); }
            if (bytes.length > MAX_BYTES || !matchesMime(bytes, mime))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File content does not match MIME type");
        }
        String request = entryId + ":" + kind + ":" + (link ? url :
            mime + ":" + fileName + ":" + hashBytes(bytes));
        Long replay = idempotency.find(student, "evidence:create", key, request);
        if (replay != null) {
            EvidenceEntity saved = evidences.findById(replay).orElseThrow(this::notFound);
            entries.accessibleForEvidence(saved.getEntryId(), student, false);
            return saved.toDto();
        }
        StudentEntryEntity entry = entries.accessibleForEvidence(entryId, student, true);
        String storageKey = link ? null : UUID.randomUUID().toString();
        if (!link) {
            try {
                Files.createDirectories(directory);
                Path stored = directory.resolve(storageKey);
                Files.write(stored, bytes, StandardOpenOption.CREATE_NEW);
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            try { Files.deleteIfExists(stored); } catch (IOException ignored) {}
                        }
                    }
                });
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Evidence storage failed");
            }
        }
        EvidenceEntity evidence = evidences.save(new EvidenceEntity(entry.getId(), student.getId(), kind,
            fileName, mime, link ? null : (long) bytes.length, storageKey, link ? url : null));
        idempotency.remember(student, "evidence:create", key, request, evidence.getId());
        return evidence.toDto();
    }

    public ResponseEntity<?> get(long id) {
        UserEntity user = current.get();
        EvidenceEntity evidence = evidences.findById(id).orElseThrow(this::notFound);
        entries.accessibleForEvidence(evidence.getEntryId(), user, false);
        if ("LINK".equals(evidence.getKind())) return ResponseEntity.ok(evidence.toDto());
        Path file = directory.resolve(evidence.getStorageKey()).normalize();
        if (!file.startsWith(directory)) throw notFound();
        try {
            byte[] bytes = Files.readAllBytes(file);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(evidence.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + safeName(evidence.getFileName()) + "\"")
                .body(new ByteArrayResource(bytes));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence file missing");
        }
    }

    private boolean validLink(String value) {
        try {
            URI uri = URI.create(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) ||
                "https".equalsIgnoreCase(uri.getScheme())) &&
                uri.getHost() != null && uri.getUserInfo() == null;
        } catch (RuntimeException e) { return false; }
    }

    private boolean matchesMime(byte[] bytes, String mime) {
        if (bytes.length < 4) return false;
        return switch (mime) {
            case "image/jpeg" -> (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216;
            case "image/png" -> (bytes[0] & 255) == 137 && bytes[1] == 80 &&
                bytes[2] == 78 && bytes[3] == 71;
            case "image/webp" -> bytes.length >= 12 && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
            case "application/pdf" -> new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("%PDF");
            case "application/msword" -> (bytes[0] & 255) == 208 && (bytes[1] & 255) == 207;
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                bytes[0] == 80 && bytes[1] == 75;
            case "text/plain" -> validUtf8(bytes);
            default -> false;
        };
    }

    private boolean validUtf8(byte[] bytes) {
        try { StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)); return true; }
        catch (CharacterCodingException e) { return false; }
    }

    private String hashBytes(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private String safeName(String value) {
        if (value == null || value.isBlank()) return "evidence";
        String name = value.replaceAll("[^a-zA-Z0-9._-]", "_");
        return name.length() > 150 ? name.substring(name.length() - 150) : name;
    }

    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND); }
}
