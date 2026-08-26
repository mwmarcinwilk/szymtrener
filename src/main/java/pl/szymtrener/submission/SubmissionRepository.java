package pl.szymtrener.submission;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Page<Submission> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Submission> findByStatusOrderByCreatedAtDesc(SubmissionStatus status, Pageable pageable);
    Page<Submission> findByTypeOrderByCreatedAtDesc(SubmissionType type, Pageable pageable);
    List<Submission> findTop5ByOrderByCreatedAtDesc();
    long countByStatus(SubmissionStatus status);
    long countByCreatedAtAfter(Instant since);
}
