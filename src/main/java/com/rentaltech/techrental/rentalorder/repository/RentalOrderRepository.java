package com.rentaltech.techrental.rentalorder.repository;

import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.finance.model.InvoiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RentalOrderRepository extends JpaRepository<RentalOrder, Long>, JpaSpecificationExecutor<RentalOrder> {
    List<RentalOrder> findByCustomer_CustomerId(Long customerCustomerId);
    List<RentalOrder> findByCustomer_CustomerIdAndOrderStatus(Long customerId, OrderStatus orderStatus);
    List<RentalOrder> findByOrderStatusAndEndDateBetween(OrderStatus orderStatus, LocalDateTime from, LocalDateTime to);
    List<RentalOrder> findByOrderStatusAndPlanEndDateBetween(OrderStatus orderStatus, LocalDateTime from, LocalDateTime to);

    long countByOrderStatusAndPlanEndDateBetween(OrderStatus orderStatus, LocalDateTime from, LocalDateTime to);

    long countByOrderStatusAndCreatedAtBetween(OrderStatus orderStatus, LocalDateTime from, LocalDateTime to);

    /**
     * Tính tiền thuê (totalPrice) cho các đơn đã có Invoice RENT_PAYMENT được thanh toán
     * Chỉ cộng totalPrice, KHÔNG bao gồm tiền cọc.
     */
    @Query("""
            SELECT COALESCE(SUM(ro.totalPrice), 0)
            FROM RentalOrder ro
            JOIN Invoice i ON i.rentalOrder = ro
            WHERE i.invoiceType = :type
              AND i.invoiceStatus = com.rentaltech.techrental.finance.model.InvoiceStatus.SUCCEEDED
              AND i.paymentDate IS NOT NULL
              AND i.paymentDate BETWEEN :start AND :end
            """)
    BigDecimal sumTotalPriceByInvoiceTypeAndPaymentDateRange(@Param("type") InvoiceType type,
                                                             @Param("start") LocalDateTime start,
                                                             @Param("end") LocalDateTime end);

    /**
     * Tính tổng tiền cọc đã nhận cho các đơn có invoice RENT_PAYMENT hoàn tất.
     */
    @Query("""
            SELECT COALESCE(SUM(ro.depositAmount), 0)
            FROM RentalOrder ro
            JOIN Invoice i ON i.rentalOrder = ro
            WHERE i.invoiceType = :type
              AND i.invoiceStatus = com.rentaltech.techrental.finance.model.InvoiceStatus.SUCCEEDED
              AND i.paymentDate IS NOT NULL
              AND i.paymentDate BETWEEN :start AND :end
            """)
    BigDecimal sumDepositAmountByInvoiceTypeAndPaymentDateRange(@Param("type") InvoiceType type,
                                                                @Param("start") LocalDateTime start,
                                                                @Param("end") LocalDateTime end);
    @Query("""
            SELECT ro FROM RentalOrder ro
            WHERE ro.orderId = COALESCE(:orderId, ro.orderId)
              AND ro.orderStatus = COALESCE(:orderStatus, ro.orderStatus)
              AND ro.customer.customerId = COALESCE(:customerId, ro.customer.customerId)
              AND ro.planStartDate >= COALESCE(:startDateFrom, ro.planStartDate)
              AND ro.planStartDate <= COALESCE(:startDateTo, ro.planStartDate)
              AND ro.planEndDate >= COALESCE(:endDateFrom, ro.planEndDate)
              AND ro.planEndDate <= COALESCE(:endDateTo, ro.planEndDate)
              AND ro.createdAt >= COALESCE(:createdAtFrom, ro.createdAt)
              AND ro.createdAt <= COALESCE(:createdAtTo, ro.createdAt)
            """)
    Page<RentalOrder> searchRentalOrders(
            @Param("orderId") Long orderId,
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("customerId") Long customerId,
            @Param("startDateFrom") LocalDateTime startDateFrom,
            @Param("startDateTo") LocalDateTime startDateTo,
            @Param("endDateFrom") LocalDateTime endDateFrom,
            @Param("endDateTo") LocalDateTime endDateTo,
            @Param("createdAtFrom") LocalDateTime createdAtFrom,
            @Param("createdAtTo") LocalDateTime createdAtTo,
            Pageable pageable);
}
