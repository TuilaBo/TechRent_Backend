package com.rentaltech.techrental.maintenance.service;

import com.rentaltech.techrental.maintenance.model.*;
import com.rentaltech.techrental.maintenance.repository.MaintenanceResultRepository;
import com.rentaltech.techrental.maintenance.repository.MaintenanceScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceResultServiceImpl implements MaintenanceResultService {

    private final MaintenanceResultRepository resultRepository;
    private final MaintenanceScheduleRepository scheduleRepository;
    private final com.rentaltech.techrental.maintenance.repository.MaintenanceItemRepository itemRepository;

    @Override
    public MaintenanceResult upsert(Long maintenanceItemId, Long maintenanceScheduleId, String result, String status) {
        MaintenanceItem item = itemRepository.findById(maintenanceItemId).orElseThrow();
        MaintenanceSchedule schedule = scheduleRepository.findById(maintenanceScheduleId).orElseThrow();

        MaintenanceResultId id = new MaintenanceResultId(maintenanceItemId, maintenanceScheduleId);
        MaintenanceResult entity = resultRepository.findById(id).orElse(
                MaintenanceResult.builder()
                        .id(id)
                        .maintenanceItem(item)
                        .maintenanceSchedule(schedule)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        entity.setResult(result);
        entity.setStatus(status);
        if (status != null && status.equalsIgnoreCase("COMPLETED")) {
            entity.setCompleteAt(LocalDateTime.now());
        }
        return resultRepository.save(entity);
    }

    @Override
    public List<MaintenanceResult> listByItem(Long maintenanceItemId) {
        return resultRepository.findByMaintenanceItem_MaintenanceItemId(maintenanceItemId);
    }

    @Override
    public List<MaintenanceResult> listBySchedule(Long maintenanceScheduleId) {
        return resultRepository.findByMaintenanceSchedule_MaintenanceScheduleId(maintenanceScheduleId);
    }
}


