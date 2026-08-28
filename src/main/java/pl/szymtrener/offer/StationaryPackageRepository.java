package pl.szymtrener.offer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StationaryPackageRepository extends JpaRepository<StationaryPackage, Long> {
    List<StationaryPackage> findByKindAndVisibleTrueOrderBySortOrderAsc(StationaryKind kind);
    List<StationaryPackage> findAllByOrderByKindAscSortOrderAsc();
}
