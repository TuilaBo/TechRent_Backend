package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.HandoverReportItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HandoverReportItemRepository extends JpaRepository<HandoverReportItem, Long> {
    Optional<HandoverReportItem> findFirstByHandoverReport_HandoverReportIdAndAllocation_Device_DeviceId(Long handoverReportId,
                                                                                                        Long deviceId);
}
