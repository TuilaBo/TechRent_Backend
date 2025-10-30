package com.rentaltech.techrental.maintenance.service;

import com.rentaltech.techrental.maintenance.model.MaintenanceResult;

import java.util.List;

public interface MaintenanceResultService {
    MaintenanceResult upsert(Long maintenanceItemId, Long maintenanceScheduleId, String result, String status);
    List<MaintenanceResult> listByItem(Long maintenanceItemId);
    List<MaintenanceResult> listBySchedule(Long maintenanceScheduleId);
}


