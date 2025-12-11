package com.rentaltech.techrental.rentalorder.repository;

import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.model.RentalOrderExtension;
import com.rentaltech.techrental.rentalorder.model.RentalOrderExtensionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface RentalOrderExtensionRepository extends JpaRepository<RentalOrderExtension, Long> {
    List<RentalOrderExtension> findByOriginalOrder(RentalOrder originalOrder);
    List<RentalOrderExtension> findByOriginalOrder_OrderId(Long orderId);
    boolean existsByOriginalOrderAndStatusIn(RentalOrder originalOrder, List<RentalOrderExtensionStatus> statuses);
}
