package com.rentaltech.techrental.rentalorder.repository;

import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RentalOrderRepository extends JpaRepository<RentalOrder, Long>, JpaSpecificationExecutor<RentalOrder> {
    List<RentalOrder> findByCustomer_CustomerId(Long customerCustomerId);
    List<RentalOrder> findByCustomer_CustomerIdAndOrderStatus(Long customerId, OrderStatus orderStatus);
    List<RentalOrder> findByOrderStatusAndEndDateBetween(OrderStatus orderStatus, LocalDateTime from, LocalDateTime to);
    List<RentalOrder> findByOrderStatusAndPlanEndDateBetween(OrderStatus orderStatus, LocalDateTime from, LocalDateTime to);
    boolean existsByParentOrder(RentalOrder parentOrder);
    List<RentalOrder> findByParentOrder(RentalOrder parentOrder);
    List<RentalOrder> findByParentOrderIsNull();
    List<RentalOrder> findByCustomer_CustomerIdAndParentOrderIsNull(Long customerId);

    @Query("""
            SELECT ro FROM RentalOrder ro
            WHERE ro.parentOrder IS NULL
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
