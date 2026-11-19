package cl.duoc.pablo_salandra_backend.evidence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "evidences")
public class EvidenceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long entryId;
    private Long ownerId;
    private String kind;
    private String fileName;
    private String mimeType;
    private Long sizeBytes;
    private String storageKey;
    private String url;
    private long createdAt;

    protected EvidenceEntity() {}

    public EvidenceEntity(Long entryId, Long ownerId, String kind, String fileName,
            String mimeType, Long sizeBytes, String storageKey, String url) {
        this.entryId = entryId;
        this.ownerId = ownerId;
        this.kind = kind;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.url = url;
        this.createdAt = System.currentTimeMillis();
    }

    public Long getId() { return id; }
    public Long getEntryId() { return entryId; }
    public String getKind() { return kind; }
    public String getFileName() { return fileName; }
    public String getMimeType() { return mimeType; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getStorageKey() { return storageKey; }
    public String getUrl() { return url; }

    public EvidenceDto toDto() {
        return new EvidenceDto(id, kind, fileName, mimeType, sizeBytes,
            storageKey == null ? null : String.valueOf(id), url,
            "LINK".equals(kind) ? "NOT_APPLICABLE" : "UPLOADED", createdAt);
    }
}
