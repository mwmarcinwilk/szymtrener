package pl.szymtrener.media;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MediaRepository extends JpaRepository<MediaFile, Long> {
    Optional<MediaFile> findByStorageKey(String storageKey);
    Optional<MediaFile> findByChecksum(String checksum);
    Page<MediaFile> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<MediaFile> findByKindOrderByCreatedAtDesc(MediaKind kind, Pageable pageable);

    long countByKind(MediaKind kind);

    /** Ile miejsca zajmuje biblioteka — naglowek strony „Media". */
    @Query("select coalesce(sum(m.sizeBytes), 0) from MediaFile m")
    long totalBytes();

    @Modifying
    @Query("update MediaFile m set m.downloadCount = m.downloadCount + 1 where m.id = :id")
    void incrementDownloads(@Param("id") Long id);
}
