package pl.szymtrener.crm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {

    /** Wszystkie pliki watku jednym zapytaniem, bez bajtow. */
    List<MessageAttachment> findByMessageIdInOrderByIdAsc(Collection<Long> messageIds);
}
