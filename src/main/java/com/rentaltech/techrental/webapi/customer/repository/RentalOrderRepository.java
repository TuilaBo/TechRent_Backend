package com.rentaltech.techrental.webapi.customer.repository;

import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.RentalOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RentalOrderRepository extends JpaRepository<RentalOrder, Long>, JpaSpecificationExecutor<RentalOrder> {
    List<RentalOrder> findByCustomer_CustomerId(Long customerCustomerId);
}
