package com.rentaltech.techrental.device.service;

import com.rentaltech.techrental.device.model.dto.BrandRequestDto;
import com.rentaltech.techrental.device.model.dto.BrandResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BrandService {
    BrandResponseDto create(BrandRequestDto request);
    BrandResponseDto findById(Long id);
    List<BrandResponseDto> findAll();
    Page<BrandResponseDto> search(String brandName, Boolean isActive, Pageable pageable);
    BrandResponseDto update(Long id, BrandRequestDto request);
    void delete(Long id);
}

