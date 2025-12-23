package com.rentaltech.techrental.finance.repository;

import com.rentaltech.techrental.finance.model.Invoice;
import com.rentaltech.techrental.finance.model.InvoiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findFirstByRentalOrder_OrderIdOrderByInvoiceIdDesc(Long rentalOrderId);
    List<Invoice> findByRentalOrder_OrderIdOrderByInvoiceIdDesc(Long rentalOrderId);
    Optional<Invoice> findByPayosOrderCode(Long payosOrderCode);
    boolean existsByPayosOrderCode(Long payosOrderCode);
    Optional<Invoice> findByVnpayTransactionId(String vnpayTransactionId);
    
    @Query("""
            SELECT COALESCE(SUM(i.totalAmount), 0)
            FROM Invoice i
            WHERE i.invoiceType = :type
              AND i.invoiceStatus = 'SUCCEEDED'
              AND i.paymentDate IS NOT NULL
              AND i.paymentDate BETWEEN :start AND :end
            """)
    BigDecimal sumTotalAmountByTypeAndPaymentDateRange(@Param("type") InvoiceType type,
                                                        @Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);
    
    /**
     * Tính tiền thuê thực tế (totalPrice) từ Invoice RENT_PAYMENT
     * Lấy từ RentalOrder.totalPrice để tránh double counting với depositAmount
     * Lưu ý: Invoice.totalAmount = depositAmount + totalPrice, nhưng depositApplied = 0
     * Nên cần lấy trực tiếp từ RentalOrder.totalPrice
     */
    @Query("""
            SELECT COALESCE(SUM(o.totalPrice), 0)
            FROM Invoice i
            JOIN i.rentalOrder o
            WHERE i.invoiceType = 'RENT_PAYMENT'
              AND i.invoiceStatus = 'SUCCEEDED'
              AND i.paymentDate IS NOT NULL
              AND i.paymentDate BETWEEN :start AND :end
            """)
    BigDecimal sumRentalPriceExcludingDeposit(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);
}
