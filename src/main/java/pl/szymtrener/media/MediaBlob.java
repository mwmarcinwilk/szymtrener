package pl.szymtrener.media;

import jakarta.persistence.*;

/** Bajty w osobnej tabeli — lista mediow w panelu nigdy ich nie zaciaga. */
@Entity
@Table(name = "media_blob")
public class MediaBlob {
    @Id
    @Column(name = "media_id")
    private Long mediaId;

    @Column(name = "data", nullable = false, columnDefinition = "bytea")
    private byte[] data;

    protected MediaBlob() {}

    public MediaBlob(Long mediaId, byte[] data) {
        this.mediaId = mediaId;
        this.data = data;
    }

    public Long getMediaId() { return mediaId; }
    public byte[] getData() { return data; }
}
