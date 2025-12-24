package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.DeviceReplacementReportItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceReplacementReportItemRepository extends JpaRepository<DeviceReplacementReportItem, Long> {
    
    /**
     * Tìm tất cả DeviceReplacementReportItem đang reference allocation này
     */
    @Query("SELECT item FROM DeviceReplacementReportItem item WHERE item.allocation.allocationId = :allocationId")
    List<DeviceReplacementReportItem> findByAllocation_AllocationId(@Param("allocationId") Long allocationId);
}

