package com.rentaltech.techrental.device.repository;

import com.rentaltech.techrental.device.model.ConditionDefinition;
import com.rentaltech.techrental.device.model.ConditionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConditionDefinitionRepository extends JpaRepository<ConditionDefinition, Long> {
    List<ConditionDefinition> findByDeviceModel_DeviceModelId(Long deviceModelId);
    boolean existsByNameIgnoreCase(String name);
    Optional<ConditionDefinition> findFirstByDeviceModel_DeviceModelIdAndConditionType(Long deviceModelId, ConditionType conditionType);
}
