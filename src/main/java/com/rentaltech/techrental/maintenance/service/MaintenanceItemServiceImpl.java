package com.rentaltech.techrental.maintenance.service;

import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.repository.DeviceRepository;
import com.rentaltech.techrental.maintenance.model.MaintenanceItem;
import com.rentaltech.techrental.maintenance.repository.MaintenanceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceItemServiceImpl implements MaintenanceItemService {

    private final MaintenanceItemRepository maintenanceItemRepository;
    private final DeviceRepository deviceRepository;

    @Override
    public MaintenanceItem create(Long deviceId, String description, Integer estimateTime, String status, String outcomeNote) {
        Device device = deviceRepository.findById(deviceId).orElseThrow();
        MaintenanceItem item = MaintenanceItem.builder()
                .device(device)
                .description(description)
                .estimateTime(estimateTime)
                .status(status)
                .outcomeNote(outcomeNote)
                .createdAt(LocalDateTime.now())
                .build();
        return maintenanceItemRepository.save(item);
    }

    @Override
    public List<MaintenanceItem> listByDevice(Long deviceId) {
        return maintenanceItemRepository.findByDevice_DeviceId(deviceId);
    }

    @Override
    public MaintenanceItem updateStatus(Long maintenanceItemId, String status) {
        MaintenanceItem item = maintenanceItemRepository.findById(maintenanceItemId).orElseThrow();
        item.setStatus(status);
        if (status != null && status.equalsIgnoreCase("COMPLETED")) {
            item.setCompletedAt(LocalDateTime.now());
        }
        return maintenanceItemRepository.save(item);
    }
}


