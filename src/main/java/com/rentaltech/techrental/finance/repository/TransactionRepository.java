package com.rentaltech.techrental.finance.repository;

import com.rentaltech.techrental.finance.model.Invoice;
import com.rentaltech.techrental.finance.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    boolean existsByInvoice(Invoice invoice);
    List<Transaction> findAllByOrderByCreatedAtDesc();
}
