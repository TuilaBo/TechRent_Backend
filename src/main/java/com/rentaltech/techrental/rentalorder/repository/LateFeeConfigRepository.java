package com.rentaltech.techrental.rentalorder.repository;

import com.rentaltech.techrental.rentalorder.model.LateFeeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LateFeeConfigRepository extends JpaRepository<LateFeeConfig, Long> {
    Optional<LateFeeConfig> findTopByOrderByIdAsc();
}
