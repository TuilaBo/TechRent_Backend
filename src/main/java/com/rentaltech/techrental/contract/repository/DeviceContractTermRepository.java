package com.rentaltech.techrental.contract.repository;

import com.rentaltech.techrental.contract.model.DeviceContractTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DeviceContractTermRepository extends JpaRepository<DeviceContractTerm, Long> {

    List<DeviceContractTerm> findByDevice_DeviceIdInAndActiveIsTrue(Collection<Long> deviceIds);

    List<DeviceContractTerm> findByDeviceCategory_DeviceCategoryIdInAndActiveIsTrue(Collection<Long> categoryIds);

    List<DeviceContractTerm> findByDeviceIsNullAndDeviceCategoryIsNullAndActiveIsTrue();

    List<DeviceContractTerm> findByDevice_DeviceId(Long deviceId);

    List<DeviceContractTerm> findByDeviceCategory_DeviceCategoryId(Long categoryId);
}

