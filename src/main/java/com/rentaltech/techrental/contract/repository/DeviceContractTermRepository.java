package com.rentaltech.techrental.contract.repository;

import com.rentaltech.techrental.contract.model.DeviceContractTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DeviceContractTermRepository extends JpaRepository<DeviceContractTerm, Long> {

    List<DeviceContractTerm> findByDeviceModel_DeviceModelIdInAndActiveIsTrue(Collection<Long> deviceModelIds);

    List<DeviceContractTerm> findByDeviceCategory_DeviceCategoryIdInAndActiveIsTrue(Collection<Long> categoryIds);

    List<DeviceContractTerm> findByDeviceModelIsNullAndDeviceCategoryIsNullAndActiveIsTrue();

    List<DeviceContractTerm> findByDeviceModel_DeviceModelId(Long deviceModelId);

    List<DeviceContractTerm> findByDeviceCategory_DeviceCategoryId(Long categoryId);
}

