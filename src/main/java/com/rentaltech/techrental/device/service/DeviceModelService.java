package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.dto.DeviceModelRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceModelResponseDto;

import java.util.List;

public interface DeviceModelService {
    DeviceModelResponseDto create(DeviceModelRequestDto request);
    DeviceModelResponseDto findById(Long id);
    List<DeviceModelResponseDto> findAll();
    DeviceModelResponseDto update(Long id, DeviceModelRequestDto request);
    void delete(Long id);
}
