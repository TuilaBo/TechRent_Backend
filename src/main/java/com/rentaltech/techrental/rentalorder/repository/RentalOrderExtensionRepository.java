package com.rentaltech.techrental.rentalorder.repository;

import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.model.RentalOrderExtension;
import com.rentaltech.techrental.rentalorder.model.RentalOrderExtensionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RentalOrderExtensionRepository extends JpaRepository<RentalOrderExtension, Long> {
    List<RentalOrderExtension> findByRentalOrder_OrderIdOrderByExtensionStartAsc(Long rentalOrderId);

    List<RentalOrderExtension> findByRentalOrder_OrderIdAndStatus(Long rentalOrderId, RentalOrderExtensionStatus status);

    boolean existsByRentalOrder(RentalOrder rentalOrder);
}
