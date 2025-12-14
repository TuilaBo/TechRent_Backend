package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.DeviceReplacementReport;
import com.rentaltech.techrental.staff.model.DeviceReplacementReportStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceReplacementReportRepository extends JpaRepository<DeviceReplacementReport, Long> {
    List<DeviceReplacementReport> findByTask_TaskId(Long taskId);
    List<DeviceReplacementReport> findByRentalOrder_OrderId(Long orderId);
    
    @Query("""
            select drr from DeviceReplacementReport drr
            join drr.task t
            join t.assignedStaff staff
            where staff.staffId = :staffId
            """)
    List<DeviceReplacementReport> findByStaff(@Param("staffId") Long staffId);
    
    List<DeviceReplacementReport> findByRentalOrder_Customer_CustomerId(Long customerId);
    
    /**
     * Tìm biên bản pending của order (chưa ký, có thể thêm items vào)
     * Sử dụng pessimistic lock để tránh race condition
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select drr from DeviceReplacementReport drr
            where drr.rentalOrder.orderId = :orderId
            and drr.status = :status
            and drr.staffSigned = false
            and drr.customerSigned = false
            order by drr.createdAt asc
            """)
    Optional<DeviceReplacementReport> findPendingReportByOrderId(
            @Param("orderId") Long orderId,
            @Param("status") DeviceReplacementReportStatus status
    );
}

