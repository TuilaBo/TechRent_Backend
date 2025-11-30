package com.rentaltech.techrental.device.repository;

import com.rentaltech.techrental.device.model.ConditionDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConditionDefinitionRepository extends JpaRepository<ConditionDefinition, Long> {
    List<ConditionDefinition> findByDeviceModel_DeviceModelId(Long deviceModelId);
    boolean existsByNameIgnoreCase(String name);
}
