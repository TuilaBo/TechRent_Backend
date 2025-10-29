package com.rentaltech.techrental.maintenance.repository;

import com.rentaltech.techrental.maintenance.model.MaintenanceResult;
import com.rentaltech.techrental.maintenance.model.MaintenanceResultId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceResultRepository extends JpaRepository<MaintenanceResult, MaintenanceResultId> {
    List<MaintenanceResult> findByMaintenanceItem_MaintenanceItemId(Long maintenanceItemId);
    List<MaintenanceResult> findByMaintenanceSchedule_MaintenanceScheduleId(Long maintenanceScheduleId);
}


