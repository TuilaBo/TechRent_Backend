package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.dto.DeviceRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceResponseDto;

import java.util.List;

public interface DeviceService {
    DeviceResponseDto create(DeviceRequestDto request);
    DeviceResponseDto findById(Long id);
    List<DeviceResponseDto> findAll();
    DeviceResponseDto update(Long id, DeviceRequestDto request);
    void delete(Long id);
}
