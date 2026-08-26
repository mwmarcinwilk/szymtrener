package pl.szymtrener.crm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TraineeRepository extends JpaRepository<Trainee, Long> {

    /**
     * Klienci bez daty startu maja isc na koniec listy, nie na gore.
     * „nulls last" nie da sie zapisac w nazwie metody — stad jawne zapytanie.
     */
    @Query("select t from Trainee t order by t.startedAt desc nulls last, t.id desc")
    Page<Trainee> findAllOrdered(Pageable pageable);

    @Query("select t from Trainee t where t.status = :status order by t.startedAt desc nulls last, t.id desc")
    Page<Trainee> findByStatusOrdered(@Param("status") TraineeStatus status, Pageable pageable);

    long countByStatus(TraineeStatus status);

    Optional<Trainee> findBySubmissionId(Long submissionId);

    long countByMode(TraineeMode mode);
}
