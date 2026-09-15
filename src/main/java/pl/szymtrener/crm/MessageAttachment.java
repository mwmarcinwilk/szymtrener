package pl.szymtrener.crm;

import jakarta.persistence.*;

import java.time.Instant;
import pl.szymtrener.common.Bytes;

/** Plik przy wiadomosci w watku. Bajty leza w {@link MessageAttachmentBlob}. */
@Entity
@Table(name = "message_attachment")
public class MessageAttachment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "message_id", nullable = false) private Long messageId;
    @Column(name = "original_name", nullable = false) private String originalName;
    @Column(name = "mime_type", nullable = false) private String mimeType;
    @Column(name = "size_bytes", nullable = false) private long sizeBytes;
    @Column(nullable = false) private String sha256;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    protected MessageAttachment() {}

    MessageAttachment(Long messageId, String originalName, String mimeType, long sizeBytes, String sha256) {
        this.messageId = messageId;
        this.originalName = originalName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
    }

    /** Miniatura w watku tylko dla obrazow rozpoznanych po tresci; SVG i HTML nigdy. */
    @Transient
    public boolean previewable() {
        return AttachmentPolicy.PREVIEWABLE.contains(mimeType);
    }

    @Transient
    public String sizeLabel() {
        return Bytes.human(sizeBytes);
    }

    public Long getId() { return id; }
    public Long getMessageId() { return messageId; }
    public String getOriginalName() { return originalName; }
    public String getMimeType() { return mimeType; }
    public String getSha256() { return sha256; }
}
