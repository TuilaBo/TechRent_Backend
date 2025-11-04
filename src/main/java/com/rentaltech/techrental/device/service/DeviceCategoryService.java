package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.dto.DeviceCategoryRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceCategoryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DeviceCategoryService {
    DeviceCategoryResponseDto create(DeviceCategoryRequestDto request);
    DeviceCategoryResponseDto findById(Long id);
    List<DeviceCategoryResponseDto> findAll();
    Page<DeviceCategoryResponseDto> search(String deviceCategoryName,
                                           Boolean isActive,
                                           Pageable pageable);
    DeviceCategoryResponseDto update(Long id, DeviceCategoryRequestDto request);
    void delete(Long id);
}
