package com.rentaltech.techrental.webapi.customer.service;

import com.rentaltech.techrental.webapi.customer.model.dto.BankInformationRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.BankInformationResponseDto;

import java.util.List;

public interface BankInformationService {
    BankInformationResponseDto create(BankInformationRequestDto request);
    BankInformationResponseDto findById(Long id);
    List<BankInformationResponseDto> findAll();
    BankInformationResponseDto update(Long id, BankInformationRequestDto request);
    void delete(Long id);
}

