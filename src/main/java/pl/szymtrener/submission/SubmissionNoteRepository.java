package pl.szymtrener.submission;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionNoteRepository extends JpaRepository<SubmissionNote, Long> {

    /** Przypiete na gorze, potem od najnowszej — kolejnosc z handoffu. */
    List<SubmissionNote> findBySubmissionIdOrderByPinnedDescCreatedAtDesc(Long submissionId);

    List<SubmissionNote> findByTraineeIdOrderByPinnedDescCreatedAtDesc(Long traineeId);

    List<SubmissionNote> findBySubmissionIdOrderByCreatedAtDesc(Long submissionId);
}
