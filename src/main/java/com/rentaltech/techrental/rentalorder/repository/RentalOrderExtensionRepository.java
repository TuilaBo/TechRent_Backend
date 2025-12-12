package com.rentaltech.techrental.rentalorder.repository;

import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.model.RentalOrderExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RentalOrderExtensionRepository extends JpaRepository<RentalOrderExtension, Long> {
    List<RentalOrderExtension> findByRentalOrder_OrderIdOrderByExtensionStartAsc(Long rentalOrderId);
    boolean existsByRentalOrder(RentalOrder rentalOrder);
}
