package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.dto.DeviceModelRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceModelResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface DeviceModelService {
    DeviceModelResponseDto create(DeviceModelRequestDto request);
    DeviceModelResponseDto findById(Long id);
    List<DeviceModelResponseDto> findAll();
    Page<DeviceModelResponseDto> search(String deviceName,
                                        String brand,
                                        Long deviceCategoryId,
                                        Boolean isActive,
                                        Pageable pageable);
    DeviceModelResponseDto update(Long id, DeviceModelRequestDto request);
    void delete(Long id);
}
