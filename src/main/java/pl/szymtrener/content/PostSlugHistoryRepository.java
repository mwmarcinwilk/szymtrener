package pl.szymtrener.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostSlugHistoryRepository extends JpaRepository<PostSlugHistory, String> {
    Optional<PostSlugHistory> findBySlug(String slug);
    void deleteBySlug(String slug);
}
