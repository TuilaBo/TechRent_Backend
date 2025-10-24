package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.dto.DeviceCategoryRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceCategoryResponseDto;

import java.util.List;

public interface DeviceCategoryService {
    DeviceCategoryResponseDto create(DeviceCategoryRequestDto request);
    DeviceCategoryResponseDto findById(Long id);
    List<DeviceCategoryResponseDto> findAll();
    DeviceCategoryResponseDto update(Long id, DeviceCategoryRequestDto request);
    void delete(Long id);
}
