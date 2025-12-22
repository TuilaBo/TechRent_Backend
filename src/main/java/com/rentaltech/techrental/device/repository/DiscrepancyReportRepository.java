package com.rentaltech.techrental.device.repository;

import com.rentaltech.techrental.device.model.DiscrepancyCreatedFrom;
import com.rentaltech.techrental.device.model.DiscrepancyReport;
import com.rentaltech.techrental.device.model.DiscrepancyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface DiscrepancyReportRepository extends JpaRepository<DiscrepancyReport, Long> {
    List<DiscrepancyReport> findByCreatedFromAndRefIdOrderByCreatedAtDesc(DiscrepancyCreatedFrom createdFrom, Long refId);

    List<DiscrepancyReport> findByAllocation_OrderDetail_RentalOrder_OrderId(Long orderId);

    long countByDiscrepancyTypeAndCreatedAtBetween(DiscrepancyType discrepancyType,
                                                   LocalDateTime start,
                                                   LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(d.penaltyAmount), 0)
            FROM DiscrepancyReport d
            WHERE d.createdAt BETWEEN :start AND :end
            """)
    BigDecimal sumPenaltyAmountBetween(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);
}
