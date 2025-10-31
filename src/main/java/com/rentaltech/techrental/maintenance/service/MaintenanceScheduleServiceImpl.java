package com.rentaltech.techrental.maintenance.service;

import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.maintenance.model.MaintenancePlan;
import com.rentaltech.techrental.maintenance.model.MaintenanceSchedule;
import com.rentaltech.techrental.maintenance.repository.MaintenancePlanRepository;
import com.rentaltech.techrental.maintenance.repository.MaintenanceScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceScheduleServiceImpl implements MaintenanceScheduleService {

    private final MaintenanceScheduleRepository scheduleRepository;
    private final MaintenancePlanRepository planRepository;
    private final DeviceRepository deviceRepository;

    @Override
    public MaintenanceSchedule createSchedule(Long deviceId, Long maintenancePlanId, LocalDate startDate, LocalDate endDate, String status) {
        Device device = deviceRepository.findById(deviceId).orElseThrow();

        MaintenancePlan plan = null;
        if (maintenancePlanId != null) {
            plan = planRepository.findById(maintenancePlanId).orElseThrow();
        }

        MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                .device(device)
                .maintenancePlan(plan)
                .startDate(startDate)
                .endDate(endDate)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
        return scheduleRepository.save(schedule);
    }

    @Override
    public List<MaintenanceSchedule> listByDevice(Long deviceId) {
        return scheduleRepository.findByDevice_DeviceId(deviceId);
    }

    @Override
    public MaintenanceSchedule updateStatus(Long maintenanceScheduleId, String status) {
        MaintenanceSchedule schedule = scheduleRepository.findById(maintenanceScheduleId).orElseThrow();
        schedule.setStatus(status);
        return scheduleRepository.save(schedule);
    }
}


