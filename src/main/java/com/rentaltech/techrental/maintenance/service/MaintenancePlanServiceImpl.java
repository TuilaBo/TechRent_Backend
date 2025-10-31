package com.rentaltech.techrental.maintenance.service;

import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.maintenance.model.*;
import com.rentaltech.techrental.maintenance.repository.MaintenancePlanDeviceRepository;
import com.rentaltech.techrental.maintenance.repository.MaintenancePlanRepository;
import com.rentaltech.techrental.maintenance.repository.MaintenanceScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenancePlanServiceImpl implements MaintenancePlanService {

    private final MaintenancePlanRepository planRepository;
    private final MaintenancePlanDeviceRepository planDeviceRepository;
    private final MaintenanceScheduleRepository scheduleRepository;
    private final DeviceRepository deviceRepository;

    
    @Override
    public MaintenancePlan createPlan(String status,
                                      Integer ruleValue,
                                      MaintenancePlanScopeType scopeType,
                                      MaintenanceRuleType ruleType,
                                      Boolean active,
                                      List<Long> deviceIds,
                                      LocalDate startDate,
                                      LocalDate endDate,
                                      String scheduleStatus) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            throw new IllegalArgumentException("deviceIds is required and must not be empty");
        }
        MaintenancePlan plan = MaintenancePlan.builder()
                .status(status)
                .ruleValue(ruleValue)
                .scopeType(scopeType)
                .ruleType(ruleType)
                .active(active)
                .lastGeneratedAt(null)
                .createdAt(LocalDateTime.now())
                .build();
        plan = planRepository.save(plan);

        if (deviceIds != null) {
            for (Long deviceId : deviceIds) {
                Device device = deviceRepository.findById(deviceId)
                        .orElseThrow(() -> new java.util.NoSuchElementException("Device not found: " + deviceId));
                MaintenancePlanDevice link = MaintenancePlanDevice.builder()
                        .id(new MaintenancePlanDeviceId(plan.getMaintenancePlanId(), deviceId))
                        .maintenancePlan(plan)
                        .device(device)
                        .build();
                planDeviceRepository.save(link);

                if (startDate != null && endDate != null) {
                    MaintenanceSchedule schedule = MaintenanceSchedule.builder()
                            .maintenancePlan(plan)
                            .device(device)
                            .startDate(startDate)
                            .endDate(endDate)
                            .status(scheduleStatus != null ? scheduleStatus : "PLANNED")
                            .createdAt(LocalDateTime.now())
                            .build();
                    scheduleRepository.save(schedule);
                }
            }
        }

        return plan;
    }

    @Override
    public List<MaintenancePlan> listPlans() {
        return planRepository.findAll();
    }
}


