package com.rentaltech.techrental.finance.repository;

import com.rentaltech.techrental.finance.model.Invoice;
import com.rentaltech.techrental.finance.model.Transaction;
import com.rentaltech.techrental.finance.model.TrasactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    boolean existsByInvoice(Invoice invoice);
    List<Transaction> findAllByOrderByCreatedAtDesc();
    
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.transactionType = :type
              AND t.createdAt BETWEEN :start AND :end
            """)
    BigDecimal sumAmountByTypeAndDateRange(@Param("type") TrasactionType type,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);
}
