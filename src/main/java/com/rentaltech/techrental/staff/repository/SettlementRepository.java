package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByRentalOrder_OrderId(Long orderId);
    List<Settlement> findAll();
}

