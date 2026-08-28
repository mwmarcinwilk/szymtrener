package pl.szymtrener.crm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {
    List<TrainingSession> findByTraineeIdOrderByStartsAtDesc(Long traineeId);
    List<TrainingSession> findByTraineeIdAndStatusOrderByStartsAtAsc(Long traineeId, SessionStatus status);
    List<TrainingSession> findByStartsAtBetweenOrderByStartsAtAsc(Instant from, Instant to);
}
