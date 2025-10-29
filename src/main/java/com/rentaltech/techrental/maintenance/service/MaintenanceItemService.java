package com.rentaltech.techrental.maintenance.service;

import com.rentaltech.techrental.maintenance.model.MaintenanceItem;

import java.util.List;

public interface MaintenanceItemService {
    MaintenanceItem create(Long deviceId, String description, Integer estimateTime, String status, String outcomeNote);
    List<MaintenanceItem> listByDevice(Long deviceId);
    MaintenanceItem updateStatus(Long maintenanceItemId, String status);
}


