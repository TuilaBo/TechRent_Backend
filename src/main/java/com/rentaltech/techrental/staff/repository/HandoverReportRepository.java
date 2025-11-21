package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.HandoverReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HandoverReportRepository extends JpaRepository<HandoverReport, Long> {
    List<HandoverReport> findByTask_TaskId(Long taskId);

    List<HandoverReport> findByRentalOrder_OrderId(Long orderId);

    @Query("""
            select hr from HandoverReport hr
            join hr.task t
            join t.assignedStaff staff
            where staff.staffId = :staffId
            """)
    List<HandoverReport> findByTechnician(@Param("staffId") Long staffId);

    List<HandoverReport> findByRentalOrder_Customer_CustomerId(Long customerId);
}

