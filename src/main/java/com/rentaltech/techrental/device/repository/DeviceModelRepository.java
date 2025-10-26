package com.rentaltech.techrental.device.repository;

import com.rentaltech.techrental.device.model.DeviceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceModelRepository extends JpaRepository<DeviceModel, Long>, JpaSpecificationExecutor<DeviceModel> {
}
