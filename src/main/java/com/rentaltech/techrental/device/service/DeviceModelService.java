package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.dto.DeviceModelRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceModelResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface DeviceModelService {
    DeviceModelResponseDto create(DeviceModelRequestDto request, MultipartFile imageFile);
    DeviceModelResponseDto findById(Long id);
    List<DeviceModelResponseDto> findAll();
    Page<DeviceModelResponseDto> search(String deviceName,
                                        Long brandId,
                                        Long amountAvailable,
                                        Long deviceCategoryId,
                                        BigDecimal pricePerDay,
                                        Boolean isActive,
                                        Pageable pageable);
    DeviceModelResponseDto update(Long id, DeviceModelRequestDto request, MultipartFile imageFile);
    void delete(Long id);
}
