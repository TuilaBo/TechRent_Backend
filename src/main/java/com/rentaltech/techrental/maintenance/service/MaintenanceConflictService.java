package com.rentaltech.techrental.maintenance.service;

import com.rentaltech.techrental.maintenance.model.MaintenanceConflict;
import com.rentaltech.techrental.maintenance.model.MaintenanceConflictSource;

import java.util.List;

public interface MaintenanceConflictService {
    MaintenanceConflict create(Long maintenanceScheduleId, Long bookingCalendarId, String status, MaintenanceConflictSource source);
    List<MaintenanceConflict> listBySchedule(Long maintenanceScheduleId);
}


