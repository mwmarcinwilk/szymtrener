package pl.szymtrener.crm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySubmissionIdOrderBySentAtAsc(Long submissionId);
    List<Message> findByTraineeIdOrderBySentAtAsc(Long traineeId);
    long countBySubmissionId(Long submissionId);

    boolean existsByMailMessageId(String mailMessageId);

    /**
     * Najnowsze zgloszenie z adresu nadawcy odebranej poczty. Zapytanie po tabeli zamiast
     * SubmissionRepository: crm nie moze dokladac zaleznosci do pakietu submission, bo ten
     * juz zalezy od crm (zamrozony cykl w GanwilkArchitectureTest).
     */
    @Query(value = "select id from submission where lower(email) = lower(:email) order by created_at desc limit 1",
            nativeQuery = true)
    Optional<Long> latestSubmissionIdByEmail(@Param("email") String email);

    /** Nasza wiadomosc, na ktora klient odpowiada. Tylko wychodzace: odpowiedz na odpowiedz nie liczy sie. */
    Optional<Message> findFirstByDirectionAndMailMessageIdIn(MessageDirection direction, Collection<String> ids);

    /**
     * Zgloszenia z nieprzeczytana odpowiedzia sposrod podanych (jedno zapytanie na strone listy).
     * Odpowiedz w watku zgloszenia zamienionego na klienta nalezy do listy klientow, tak samo
     * jak w licznikach menu, zeby jedna wiadomosc nie swiecila w dwoch miejscach naraz.
     */
    @Query("select distinct m.submissionId from Message m where m.unread = true and m.traineeId is null and m.submissionId in :ids")
    List<Long> unreadSubmissionIds(@Param("ids") Collection<Long> ids);

    @Query("select distinct m.traineeId from Message m where m.unread = true and m.traineeId in :ids")
    List<Long> unreadTraineeIds(@Param("ids") Collection<Long> ids);

    /** Licznik przy „Zgloszeniach": odpowiedzi w zgloszeniach, ktore nie sa jeszcze klientami. */
    @Query("select count(distinct m.submissionId) from Message m where m.unread = true and m.traineeId is null")
    long countSubmissionsWithUnread();

    @Query("select count(distinct m.traineeId) from Message m where m.unread = true and m.traineeId is not null")
    long countTraineesWithUnread();

    @Modifying
    @Query("update Message m set m.unread = false where m.unread = true and m.submissionId = :id")
    int markSubmissionRead(@Param("id") Long submissionId);

    @Modifying
    @Query("update Message m set m.unread = false where m.unread = true and m.traineeId = :id")
    int markTraineeRead(@Param("id") Long traineeId);
}
