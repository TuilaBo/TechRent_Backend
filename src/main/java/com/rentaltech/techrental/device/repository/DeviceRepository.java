package com.rentaltech.techrental.device.repository;

import com.rentaltech.techrental.device.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long>, JpaSpecificationExecutor<Device> {
    Optional<Device> findBySerialNumber(String serialNumber);

    Long countByDeviceModel_DeviceModelId(Long deviceModelId);

    // Find all devices by model for allocation selection
    List<Device> findByDeviceModel_DeviceModelId(Long deviceModelId);

    // Find all devices by device category (via deviceModel.deviceCategory)
    List<Device> findByDeviceModel_DeviceCategory_DeviceCategoryId(Long deviceCategoryId);
}
