package pl.szymtrener.content;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findBySlug(String slug);

    @EntityGraph(attributePaths = {"category", "author"})
    Page<Post> findByStatusOrderByPublishedAtDesc(PostStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "author"})
    Page<Post> findByStatusAndCategorySlugOrderByPublishedAtDesc(PostStatus status, String categorySlug, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "author"})
    Optional<Post> findBySlugAndStatus(String slug, PostStatus status);

    /** Samo id — widok artykulu doczytuje wpis juz wewnatrz swojej transakcji. */
    @Query("select p.id from Post p where p.slug = :slug and p.status = :status")
    Optional<Long> findIdBySlugAndStatus(@Param("slug") String slug, @Param("status") PostStatus status);

    List<Post> findByStatusAndPublishAtLessThanEqual(PostStatus status, Instant now);

    @EntityGraph(attributePaths = {"category", "author"})
    List<Post> findTop3ByStatusAndCategoryIdAndIdNotOrderByPublishedAtDesc(PostStatus status, Long categoryId, Long excludedId);

    @EntityGraph(attributePaths = {"category", "author"})
    List<Post> findTop3ByStatusAndIdNotOrderByPublishedAtDesc(PostStatus status, Long excludedId);

    /** Zapas do uzupelnienia listy „powiazane": czesc kandydatow bywa juz na liscie. */
    @EntityGraph(attributePaths = {"category", "author"})
    List<Post> findTop6ByStatusAndIdNotOrderByPublishedAtDesc(PostStatus status, Long excludedId);

    /**
     * Wyszukiwarka po kolumnie generowanej search_vector (indeks GIN).
     * Konfiguracja 'simple' nie zna polskiej odmiany — wpisujac „miesnie" nie
     * znajdziesz „miesni". Gdy wyniki beda za slabe, doinstaluj slownik polski
     * na serwerze bazy i podmien 'simple' tutaj oraz w definicji kolumny (V1).
     */
    @Query(value = "select * from post"
                 + " where status = 'PUBLISHED'"
                 + "   and search_vector @@ plainto_tsquery('simple', :q)"
                 + " order by ts_rank(search_vector, plainto_tsquery('simple', :q)) desc,"
                 + "          published_at desc",
           countQuery = "select count(*) from post"
                      + " where status = 'PUBLISHED'"
                      + "   and search_vector @@ plainto_tsquery('simple', :q)",
           nativeQuery = true)
    Page<Post> search(@Param("q") String query, Pageable pageable);

    /** Do sitemapy: tylko to, co potrzebne, bez ciagniecia tresci. */
    @Query("select p.slug, p.updatedAt from Post p where p.status = 'PUBLISHED' order by p.publishedAt desc")
    List<Object[]> findSlugsForSitemap();

    /** Do kanalu RSS — najnowsze opublikowane, z autorem i kategoria. */
    @EntityGraph(attributePaths = {"category", "author"})
    List<Post> findTop20ByStatusOrderByPublishedAtDesc(PostStatus status);

    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    void incrementViewCount(@Param("id") Long id);

    long countByStatus(PostStatus status);

    @Query("select p.title from Post p where p.coverMediaId = :mediaId")
    List<String> titlesWithCover(@Param("mediaId") Long mediaId);

    @EntityGraph(attributePaths = {"category", "author"})
    Page<Post> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    /**
     * Wyszukiwarka panelu — po tytule i po wszystkich statusach.
     * Swiadomie zwykly LIKE, a nie search_vector z wyszukiwarki publicznej:
     * tamta indeksuje tylko opublikowane, a w panelu szuka sie zwykle szkicu.
     */
    @EntityGraph(attributePaths = {"category", "author"})
    @Query("select p from Post p where lower(p.title) like lower(concat('%', :q, '%'))"
         + " order by p.updatedAt desc")
    Page<Post> searchByTitle(@Param("q") String query, Pageable pageable);

    /** Lista w panelu z filtrem statusu — kolejnosc ta sama co bez filtra. */
    @EntityGraph(attributePaths = {"category", "author"})
    Page<Post> findByStatusOrderByUpdatedAtDesc(PostStatus status, Pageable pageable);

    /**
     * Kalendarz publikacji: opublikowane licza sie po dacie publikacji, zaplanowane
     * po terminie. Projekcja, nie encje — kalendarz potrzebuje tylko statusu i daty,
     * a Post ma category i author jako EAGER, wiec kazda encja ciagnelaby dwa dolaczenia
     * za darmo.
     *
     * @return wiersze [PostStatus, Instant] — status i data, ktora go dotyczy
     */
    @Query("""
           select p.status, coalesce(p.publishedAt, p.publishAt) from Post p
           where (p.status = 'PUBLISHED' and p.publishedAt between :from and :to)
              or (p.status = 'SCHEDULED' and p.publishAt between :from and :to)""")
    List<Object[]> calendar(@Param("from") Instant from, @Param("to") Instant to);

    /** Najblizsze wpisy czekajace w kolejce — kafel na pulpicie. */
    @EntityGraph(attributePaths = {"category"})
    List<Post> findTop5ByStatusOrderByPublishAtAsc(PostStatus status);

    /** Ostatnio ruszane szkice — druga polowa tego samego kafla. */
    @EntityGraph(attributePaths = {"category"})
    List<Post> findTop5ByStatusOrderByUpdatedAtDesc(PostStatus status);
}
