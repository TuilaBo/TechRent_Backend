package com.rentaltech.techrental.device.repository;

import com.rentaltech.techrental.device.model.DeviceCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceConditionRepository extends JpaRepository<DeviceCondition, Long> {
    List<DeviceCondition> findByDevice_DeviceId(Long deviceId);

    List<DeviceCondition> findByDevice_DeviceIdIn(Collection<Long> deviceIds);

    void deleteByDevice_DeviceId(Long deviceId);
}
