package pl.szymtrener.media;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "media_file")
public class MediaFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Fragment adresu: 2026/08/uuid.jpg — niezmienny, wiec mozna cache'owac na zawsze. */
    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaKind kind;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    private Integer width;
    private Integer height;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "alt_text")
    private String altText;

    private String title;
    private String checksum;

    @Column(name = "download_count", nullable = false)
    private long downloadCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public MediaKind getKind() { return kind; }
    public void setKind(MediaKind kind) { this.kind = kind; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public String getAltText() { return altText; }
    public void setAltText(String altText) { this.altText = altText; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public long getDownloadCount() { return downloadCount; }
    public void setDownloadCount(long downloadCount) { this.downloadCount = downloadCount; }
    public Instant getCreatedAt() { return createdAt; }

    public String publicUrl() {
        return kind == MediaKind.PDF ? "/pliki/" + id + "/" + originalName : "/media/" + storageKey;
    }

    public String humanSize() {
        if (sizeBytes < 1024) return sizeBytes + " B";
        if (sizeBytes < 1024 * 1024) return Math.round(sizeBytes / 1024.0) + " KB";
        return String.format(java.util.Locale.forLanguageTag("pl-PL"), "%.1f MB", sizeBytes / 1048576.0);
    }
}
