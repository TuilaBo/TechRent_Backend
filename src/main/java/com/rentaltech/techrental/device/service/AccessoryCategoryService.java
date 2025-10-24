package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.dto.AccessoryCategoryRequestDto;
import com.rentaltech.techrental.device.model.dto.AccessoryCategoryResponseDto;

import java.util.List;

public interface AccessoryCategoryService {
    AccessoryCategoryResponseDto create(AccessoryCategoryRequestDto request);
    AccessoryCategoryResponseDto findById(Long id);
    List<AccessoryCategoryResponseDto> findAll();
    AccessoryCategoryResponseDto update(Long id, AccessoryCategoryRequestDto request);
    void delete(Long id);
}
