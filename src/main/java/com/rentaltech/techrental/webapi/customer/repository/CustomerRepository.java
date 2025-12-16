package com.rentaltech.techrental.webapi.customer.repository;

import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.KYCStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByAccount_AccountId(Long accountId);
    Optional<Customer> findByAccount_Username(String username);
    Optional<Customer> findByAccount_Email(String email);
    boolean existsByAccount_AccountId(Long accountId);
    
    // KYC queries
    List<Customer> findByKycStatus(KYCStatus kycStatus);
    List<Customer> findByKycStatusIn(List<KYCStatus> statuses);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}

