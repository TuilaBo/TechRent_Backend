package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.dto.ConditionDefinitionRequestDto;
import com.rentaltech.techrental.device.model.dto.ConditionDefinitionResponseDto;

import java.util.List;

public interface ConditionDefinitionService {
    ConditionDefinitionResponseDto create(ConditionDefinitionRequestDto request);
    ConditionDefinitionResponseDto update(Long id, ConditionDefinitionRequestDto request);
    ConditionDefinitionResponseDto getById(Long id);
    List<ConditionDefinitionResponseDto> getAll();
    List<ConditionDefinitionResponseDto> getByDeviceModel(Long deviceModelId);
    void delete(Long id);
}
