package com.rentaltech.techrental.maintenance.service;

import com.rentaltech.techrental.maintenance.model.MaintenanceSchedule;

import java.time.LocalDate;
import java.util.List;

public interface MaintenanceScheduleService {
    MaintenanceSchedule createSchedule(Long deviceId, Long maintenancePlanId, LocalDate startDate, LocalDate endDate, String status);
    List<MaintenanceSchedule> listByDevice(Long deviceId);
    MaintenanceSchedule updateStatus(Long maintenanceScheduleId, String status);
}


