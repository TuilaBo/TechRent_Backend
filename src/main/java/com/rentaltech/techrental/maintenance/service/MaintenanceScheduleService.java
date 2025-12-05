package com.rentaltech.techrental.maintenance.service;

import com.rentaltech.techrental.maintenance.model.MaintenanceSchedule;
import com.rentaltech.techrental.maintenance.model.MaintenanceScheduleStatus;
import com.rentaltech.techrental.maintenance.model.dto.MaintenanceScheduleByCategoryRequestDto;
import com.rentaltech.techrental.maintenance.model.dto.MaintenanceScheduleByUsageRequestDto;
import com.rentaltech.techrental.maintenance.model.dto.PriorityMaintenanceDeviceDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface MaintenanceScheduleService {
    MaintenanceSchedule createSchedule(Long deviceId, Long maintenancePlanId, LocalDate startDate, LocalDate endDate, MaintenanceScheduleStatus status);
    List<MaintenanceSchedule> listByDevice(Long deviceId);
    MaintenanceSchedule updateStatus(Long maintenanceScheduleId, MaintenanceScheduleStatus status, List<String> evidenceUrls);

    MaintenanceSchedule updateStatusWithUploads(Long maintenanceScheduleId,
                                                MaintenanceScheduleStatus status,
                                                List<String> evidenceUrls,
                                                List<MultipartFile> files);
    
    List<MaintenanceSchedule> createSchedulesByCategory(MaintenanceScheduleByCategoryRequestDto request);
    List<MaintenanceSchedule> createSchedulesByUsage(MaintenanceScheduleByUsageRequestDto request);
    List<PriorityMaintenanceDeviceDto> getPriorityMaintenanceDevices();
    List<MaintenanceSchedule> getActiveMaintenanceSchedules();
    List<MaintenanceSchedule> getInactiveMaintenanceSchedules();
    Page<MaintenanceSchedule> listByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable);
    void deleteSchedule(Long maintenanceScheduleId);

    /**
     * Lấy chi tiết một lịch bảo trì cụ thể theo ID.
     */
    MaintenanceSchedule getSchedule(Long maintenanceScheduleId);
}


