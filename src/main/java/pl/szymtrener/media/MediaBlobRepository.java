package pl.szymtrener.media;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaBlobRepository extends JpaRepository<MediaBlob, Long> {
}
