package com.rentaltech.techrental.maintenance.service;

import com.rentaltech.techrental.maintenance.model.MaintenanceSchedule;
import com.rentaltech.techrental.maintenance.model.dto.MaintenanceConflictCheckRequestDto;
import com.rentaltech.techrental.maintenance.model.dto.MaintenanceConflictResponseDto;
import com.rentaltech.techrental.maintenance.model.dto.MaintenanceScheduleByCategoryRequestDto;
import com.rentaltech.techrental.maintenance.model.dto.MaintenanceScheduleByUsageRequestDto;
import com.rentaltech.techrental.maintenance.model.dto.PriorityMaintenanceDeviceDto;

import java.time.LocalDate;
import java.util.List;

public interface MaintenanceScheduleService {
    MaintenanceSchedule createSchedule(Long deviceId, Long maintenancePlanId, LocalDate startDate, LocalDate endDate, String status);
    List<MaintenanceSchedule> listByDevice(Long deviceId);
    MaintenanceSchedule updateStatus(Long maintenanceScheduleId, String status);
    
    List<MaintenanceSchedule> createSchedulesByCategory(MaintenanceScheduleByCategoryRequestDto request);
    List<MaintenanceSchedule> createSchedulesByUsage(MaintenanceScheduleByUsageRequestDto request);
    List<MaintenanceConflictResponseDto> checkConflicts(MaintenanceConflictCheckRequestDto request);
    List<PriorityMaintenanceDeviceDto> getPriorityMaintenanceDevices();
}


