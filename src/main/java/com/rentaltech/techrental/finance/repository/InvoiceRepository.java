package com.rentaltech.techrental.finance.repository;

import com.rentaltech.techrental.finance.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findFirstByRentalOrder_OrderIdOrderByInvoiceIdDesc(Long rentalOrderId);
    Optional<Invoice> findByPayosOrderCode(Long payosOrderCode);
    boolean existsByPayosOrderCode(Long payosOrderCode);
    Optional<Invoice> findByVnpayTransactionId(String vnpayTransactionId);
}
