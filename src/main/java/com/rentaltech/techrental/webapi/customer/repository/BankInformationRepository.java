package com.rentaltech.techrental.webapi.customer.repository;

import com.rentaltech.techrental.webapi.customer.model.BankInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankInformationRepository extends JpaRepository<BankInformation, Long> {
    List<BankInformation> findByCustomer_CustomerId(Long customerId);

    boolean existsByCardNumberAndCustomer_CustomerId(String cardNumber, Long customerCustomerId);
}
