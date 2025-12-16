package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.LateFeeConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LateFeeConfigRepository extends JpaRepository<LateFeeConfig, Long> {
    Optional<LateFeeConfig> findTopByOrderByIdAsc();
}
