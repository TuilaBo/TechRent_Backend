package com.rentaltech.techrental.webapi.customer.repository;

import com.rentaltech.techrental.webapi.customer.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByAccount_AccountId(Long accountId);
    Optional<Customer> findByAccount_Username(String username);
    Optional<Customer> findByAccount_Email(String email);
    boolean existsByAccount_AccountId(Long accountId);
}

