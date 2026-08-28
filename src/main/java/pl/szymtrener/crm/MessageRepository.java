package pl.szymtrener.crm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySubmissionIdOrderBySentAtAsc(Long submissionId);
    List<Message> findByTraineeIdOrderBySentAtAsc(Long traineeId);
    long countBySubmissionId(Long submissionId);
}
