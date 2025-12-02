package com.rentaltech.techrental.device.repository;

import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AllocationRepository extends JpaRepository<Allocation, Long> {
    List<Allocation> findByOrderDetail_OrderDetailId(Long orderDetailId);

    List<Allocation> findByQcReport_QcReportId(Long qcReportId);

    List<Allocation> findByOrderDetail_RentalOrder_OrderId(Long orderId);

    Optional<Allocation> findByOrderDetail_OrderDetailIdAndDevice_SerialNumber(Long orderDetailId, String serialNumber);

    Optional<Allocation> findByOrderDetail_OrderDetailIdAndDevice_DeviceId(Long orderDetailId, Long deviceId);

    Optional<Allocation> findByOrderDetail_RentalOrder_OrderIdAndDevice_DeviceId(Long orderId, Long deviceId);

    @Query("SELECT DISTINCT a.orderDetail.rentalOrder FROM Allocation a " +
            "WHERE a.device.deviceId IN :deviceIds " +
            "AND a.orderDetail.rentalOrder.orderId <> :excludeOrderId " +
            "AND a.orderDetail.rentalOrder.orderStatus = :status " +
            "AND a.orderDetail.rentalOrder.startDate < :endTime " +
            "AND a.orderDetail.rentalOrder.endDate > :startTime")
    List<RentalOrder> findOrdersUsingDevicesInRange(@Param("deviceIds") Collection<Long> deviceIds,
                                                    @Param("excludeOrderId") Long excludeOrderId,
                                                    @Param("status") OrderStatus status,
                                                    @Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);
}
