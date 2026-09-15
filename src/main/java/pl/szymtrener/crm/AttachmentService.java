package pl.szymtrener.crm;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pl.szymtrener.common.Bytes;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pliki w watku rozmowy. Prywatne: nie ma dla nich publicznego adresu ani wpisu w bibliotece
 * mediow, pobiera je {@code AdminAttachmentController} tylko po zalogowaniu.
 */
@Service
public class AttachmentService {

    /** Plik z bajtami do pobrania. */
    public record Loaded(MessageAttachment attachment, byte[] data) {}

    private final MessageAttachmentRepository attachments;
    private final MessageAttachmentBlobRepository blobs;

    public AttachmentService(MessageAttachmentRepository attachments, MessageAttachmentBlobRepository blobs) {
        this.attachments = attachments;
        this.blobs = blobs;
    }

    /** Zapis w transakcji wolajacego: wiadomosc i jej pliki powstaja razem albo wcale. */
    @Transactional
    public void store(Long messageId, List<AttachmentFile> files) {
        for (AttachmentFile file : files) {
            MessageAttachment saved = attachments.save(new MessageAttachment(messageId, file.name(), file.mimeType(),
                    file.data().length, Bytes.sha256(file.data())));
            blobs.save(new MessageAttachmentBlob(saved.getId(), file.data()));
        }
    }

    /** Pliki calego watku jednym zapytaniem, pogrupowane po wiadomosci. */
    @Transactional(readOnly = true)
    public Map<Long, List<MessageAttachment>> forMessages(Collection<Long> messageIds) {
        if (messageIds.isEmpty()) return Map.of();
        return attachments.findByMessageIdInOrderByIdAsc(messageIds).stream()
                .collect(Collectors.groupingBy(MessageAttachment::getMessageId));
    }

    /** Sam opis pliku, bez bajtow — do odpowiedzi 304 na podglad, ktory przegladarka juz ma. */
    @Transactional(readOnly = true)
    public Optional<MessageAttachment> describe(Long id) {
        return attachments.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Loaded> load(Long id) {
        return attachments.findById(id)
                .flatMap(a -> blobs.findById(id).map(b -> new Loaded(a, b.getData())));
    }

}
