package com.rentaltech.techrental.device.repository;

import com.rentaltech.techrental.device.model.DeviceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceCategoryRepository extends JpaRepository<DeviceCategory, Long>, JpaSpecificationExecutor<DeviceCategory> {
}
