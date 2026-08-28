package pl.szymtrener.crm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TrainingPackageRepository extends JpaRepository<TrainingPackage, Long> {
    List<TrainingPackage> findByTraineeIdOrderByPurchasedAtDesc(Long traineeId);
    List<TrainingPackage> findByTraineeIdAndActiveTrue(Long traineeId);
    List<TrainingPackage> findByPurchasedAtGreaterThanEqual(LocalDate from);
}
