package com.rentaltech.techrental.webapi.customer.service;

import com.rentaltech.techrental.webapi.customer.model.dto.ShippingAddressRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.ShippingAddressResponseDto;

import java.util.List;

public interface ShippingAddressService {
    ShippingAddressResponseDto create(ShippingAddressRequestDto request);
    ShippingAddressResponseDto findById(Long id);
    List<ShippingAddressResponseDto> findAll();
    ShippingAddressResponseDto update(Long id, ShippingAddressRequestDto request);
    void delete(Long id);
}

