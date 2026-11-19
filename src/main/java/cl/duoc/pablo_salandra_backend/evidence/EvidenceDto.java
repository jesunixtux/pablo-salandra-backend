package cl.duoc.pablo_salandra_backend.evidence;

public record EvidenceDto(
    Long id,
    String kind,
    String fileName,
    String mimeType,
    Long sizeBytes,
    String remoteRef,
    String url,
    String transferState,
    long createdAt
) {}
