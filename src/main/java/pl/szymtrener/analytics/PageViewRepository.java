package pl.szymtrener.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PageViewRepository extends JpaRepository<PageView, Long> {

    /** Retencja RODO: statystyka starsza niz rok nie ma juz zadnej wartosci. */
    @Modifying
    @Query("delete from PageView v where v.viewedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);

    @Query("select count(distinct v.sessionHash) from PageView v where v.bot = false and v.viewedAt > :since")
    long countSessions(@Param("since") Instant since);

    @Query("select count(v) from PageView v where v.bot = false and v.viewedAt > :since")
    long countViews(@Param("since") Instant since);

    @Query("""
           select v.path, count(v) from PageView v
           where v.bot = false and v.viewedAt > :since
           group by v.path order by count(v) desc""")
    List<Object[]> topPaths(@Param("since") Instant since);

    @Query("""
           select v.referrer, count(v) from PageView v
           where v.bot = false and v.viewedAt > :since and v.referrer is not null
           group by v.referrer order by count(v) desc""")
    List<Object[]> topReferrers(@Param("since") Instant since);

    /** Odslony dzien po dniu — dane pod wykres slupkowy w panelu. */
    @Query(value = "select date_trunc('day', viewed_at) as d, count(*)"
                 + " from page_view where is_bot = false and viewed_at > :since"
                 + " group by d order by d", nativeQuery = true)
    List<Object[]> dailyViews(@Param("since") Instant since);

    /**
     * Okno zamkniete z obu stron — potrzebne do porownania „ostatnie 30 dni
     * kontra 30 dni wczesniej". Bez tego trend na kafelku nie ma z czym porownac.
     */
    @Query("select count(v) from PageView v where v.bot = false and v.viewedAt between :from and :to")
    long countViewsBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(distinct v.sessionHash) from PageView v"
         + " where v.bot = false and v.viewedAt between :from and :to")
    long countSessionsBetween(@Param("from") Instant from, @Param("to") Instant to);

    /** Podzial na urzadzenia — telefon kontra komputer. */
    @Query("""
           select v.device, count(v) from PageView v
           where v.bot = false and v.viewedAt > :since and v.device is not null
           group by v.device order by count(v) desc""")
    List<Object[]> deviceSplit(@Param("since") Instant since);

    /** Osobno: ile razy zajrzaly boty AI — to jest realny wskaznik widocznosci. */
    @Query("""
           select v.botName, count(v) from PageView v
           where v.bot = true and v.botName is not null and v.viewedAt > :since
           group by v.botName order by count(v) desc""")
    List<Object[]> botVisits(@Param("since") Instant since);
}
