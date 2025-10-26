package com.rentaltech.techrental.webapi.customer.service;

import com.rentaltech.techrental.webapi.customer.model.dto.RentalOrderRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.RentalOrderResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface RentalOrderService {
    RentalOrderResponseDto create(RentalOrderRequestDto request);
    RentalOrderResponseDto findById(Long id);
    List<RentalOrderResponseDto> findAll();
    Page<RentalOrderResponseDto> search(
            String orderStatus,
            Long customerId,
            String shippingAddress,
            Double minTotalPrice,
            Double maxTotalPrice,
            Double minPricePerDay,
            Double maxPricePerDay,
            String startDateFrom,
            String startDateTo,
            String endDateFrom,
            String endDateTo,
            String createdAtFrom,
            String createdAtTo,
            Pageable pageable);
    RentalOrderResponseDto update(Long id, RentalOrderRequestDto request);
    void delete(Long id);
}
