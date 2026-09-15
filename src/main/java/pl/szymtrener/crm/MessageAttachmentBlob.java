package pl.szymtrener.crm;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

/**
 * Bajty zalacznika w osobnej tabeli, zeby lista plikow w watku ich nie zaciagala.
 * Persistable z isNew() = true: identyfikator ustawiamy sami, wiec bez tego Spring Data wolalby
 * merge(), czyli zbedny SELECT i kopie tablicy bajtow (do 15 MB) przed kazdym zapisem.
 */
@Entity
@Table(name = "message_attachment_blob")
public class MessageAttachmentBlob implements Persistable<Long> {

    @Id
    @Column(name = "attachment_id")
    private Long attachmentId;

    @Column(name = "data", nullable = false, columnDefinition = "bytea")
    private byte[] data;

    protected MessageAttachmentBlob() {}

    MessageAttachmentBlob(Long attachmentId, byte[] data) {
        this.attachmentId = attachmentId;
        this.data = data;
    }

    public byte[] getData() { return data; }

    @Override
    public Long getId() { return attachmentId; }

    /** Bajty zalacznika nigdy sie nie zmieniaja: kazdy zapis to nowy wiersz. */
    @Override
    public boolean isNew() { return true; }
}
