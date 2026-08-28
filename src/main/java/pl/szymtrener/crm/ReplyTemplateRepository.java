package pl.szymtrener.crm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReplyTemplateRepository extends JpaRepository<ReplyTemplate, Long> {
    List<ReplyTemplate> findAllByOrderBySortOrderAsc();
    Optional<ReplyTemplate> findByCode(String code);
}
