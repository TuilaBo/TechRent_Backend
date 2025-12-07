package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.DeviceReplacementReportItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceReplacementReportItemRepository extends JpaRepository<DeviceReplacementReportItem, Long> {
}

