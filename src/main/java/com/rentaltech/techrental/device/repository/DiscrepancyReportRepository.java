package com.rentaltech.techrental.device.repository;

import com.rentaltech.techrental.device.model.DiscrepancyCreatedFrom;
import com.rentaltech.techrental.device.model.DiscrepancyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscrepancyReportRepository extends JpaRepository<DiscrepancyReport, Long> {
    List<DiscrepancyReport> findByCreatedFromAndRefIdOrderByCreatedAtDesc(DiscrepancyCreatedFrom createdFrom, Long refId);

    List<DiscrepancyReport> findByAllocation_OrderDetail_RentalOrder_OrderId(Long orderId);
}
