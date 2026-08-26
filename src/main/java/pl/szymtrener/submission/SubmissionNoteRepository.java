package pl.szymtrener.submission;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionNoteRepository extends JpaRepository<SubmissionNote, Long> {
    List<SubmissionNote> findBySubmissionIdOrderByCreatedAtDesc(Long submissionId);
}
