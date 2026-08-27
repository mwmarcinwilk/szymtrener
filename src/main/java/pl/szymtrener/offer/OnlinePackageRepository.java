package pl.szymtrener.offer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnlinePackageRepository extends JpaRepository<OnlinePackage, Long> {
    List<OnlinePackage> findByVisibleTrueOrderBySortOrderAsc();
    List<OnlinePackage> findAllByOrderBySortOrderAsc();
}
