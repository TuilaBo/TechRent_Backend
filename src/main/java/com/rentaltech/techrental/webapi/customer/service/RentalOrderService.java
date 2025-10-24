package com.rentaltech.techrental.webapi.customer.service;

import com.rentaltech.techrental.webapi.customer.model.dto.RentalOrderRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.RentalOrderResponseDto;

import java.util.List;

public interface RentalOrderService {
    RentalOrderResponseDto create(RentalOrderRequestDto request);
    RentalOrderResponseDto findById(Long id);
    List<RentalOrderResponseDto> findAll();
    RentalOrderResponseDto update(Long id, RentalOrderRequestDto request);
    void delete(Long id);
}
