package com.rentaltech.techrental.maintenance.repository;

import com.rentaltech.techrental.maintenance.model.MaintenancePlanDevice;
import com.rentaltech.techrental.maintenance.model.MaintenancePlanDeviceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenancePlanDeviceRepository extends JpaRepository<MaintenancePlanDevice, MaintenancePlanDeviceId> {
    List<MaintenancePlanDevice> findByMaintenancePlan_MaintenancePlanId(Long maintenancePlanId);
}




