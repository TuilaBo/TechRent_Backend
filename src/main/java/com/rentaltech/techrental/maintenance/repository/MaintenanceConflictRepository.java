package com.rentaltech.techrental.maintenance.repository;

import com.rentaltech.techrental.maintenance.model.MaintenanceConflict;
import com.rentaltech.techrental.maintenance.model.MaintenanceConflictId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceConflictRepository extends JpaRepository<MaintenanceConflict, MaintenanceConflictId> {
    List<MaintenanceConflict> findByMaintenanceSchedule_MaintenanceScheduleId(Long maintenanceScheduleId);
}


