package com.rentaltech.techrental.device.repository;

import com.rentaltech.techrental.device.model.DeviceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceModelRepository extends JpaRepository<DeviceModel, Long>, JpaSpecificationExecutor<DeviceModel> {
    List<DeviceModel> findByDeviceCategory_DeviceCategoryId(Long deviceCategoryId);

    Optional<DeviceModel> findByDeviceNameIgnoreCase(String deviceName);
}
