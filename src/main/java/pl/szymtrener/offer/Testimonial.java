package pl.szymtrener.offer;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Opinia klienta. Podpis pod imieniem (format wspolpracy + czas trwania) jest
 * osobnymi polami, bo brief traktuje go jako osobny dowod — i oba moga byc puste,
 * wtedy podpis sie nie renderuje.
 */
@Entity
@Table(name = "testimonial")
public class Testimonial {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String name;
    private String city;
    @Column(name = "cooperation_format") private String cooperationFormat;
    @Column(name = "duration_label") private String durationLabel;
    @Column(nullable = false, columnDefinition = "text") private String body;
    @Column(name = "media_id") private Long mediaId;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false) private boolean visible = true;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    /** Podpis renderujemy tylko wtedy, gdy jest z czego go zlozyc. */
    @Transient
    public String signature() {
        boolean hasFormat = cooperationFormat != null && !cooperationFormat.isBlank();
        boolean hasDuration = durationLabel != null && !durationLabel.isBlank();
        if (hasFormat && hasDuration) return cooperationFormat + " · " + durationLabel;
        if (hasFormat) return cooperationFormat;
        return hasDuration ? durationLabel : null;
    }

    @Transient
    public String initial() {
        return name == null || name.isBlank() ? "?" : name.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCooperationFormat() { return cooperationFormat; }
    public void setCooperationFormat(String v) { this.cooperationFormat = v; }
    public String getDurationLabel() { return durationLabel; }
    public void setDurationLabel(String v) { this.durationLabel = v; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
}
