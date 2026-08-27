package pl.szymtrener.offer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnlineFaqRepository extends JpaRepository<OnlineFaq, Long> {
    List<OnlineFaq> findByVisibleTrueOrderBySortOrderAsc();
    List<OnlineFaq> findAllByOrderBySortOrderAsc();
}
