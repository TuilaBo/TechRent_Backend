package com.rentaltech.techrental.webapi.customer.repository;

import com.rentaltech.techrental.webapi.customer.model.RentalOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RentalOrderRepository extends JpaRepository<RentalOrder, Long> {
}
