package pl.szymtrener.crm;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageAttachmentBlobRepository extends JpaRepository<MessageAttachmentBlob, Long> {
}
