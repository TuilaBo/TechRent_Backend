package com.rentaltech.techrental.device.repository;

import com.rentaltech.techrental.device.model.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
