package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.dto.DeviceConditionRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceConditionResponseDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface DeviceConditionService {
    List<DeviceConditionResponseDto> getByDevice(Long deviceId);

    Map<Long, List<DeviceConditionResponseDto>> getByDeviceIds(Collection<Long> deviceIds);

    List<DeviceConditionResponseDto> upsertConditions(Long deviceId,
                                                     List<DeviceConditionRequestDto> requests,
                                                     String capturedByUsername);

    void updateCondition(Long deviceId,
                         Long conditionDefinitionId,
                         String severity,
                         String note,
                         List<String> images,
                         Long capturedByStaffId);

    void deleteByDevice(Long deviceId);
}
