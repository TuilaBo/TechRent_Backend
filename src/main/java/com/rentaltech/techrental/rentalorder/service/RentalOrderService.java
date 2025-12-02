package com.rentaltech.techrental.rentalorder.service;

import com.rentaltech.techrental.rentalorder.model.dto.RentalOrderExtendRequestDto;
import com.rentaltech.techrental.rentalorder.model.dto.RentalOrderRequestDto;
import com.rentaltech.techrental.rentalorder.model.dto.RentalOrderResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface RentalOrderService {
    RentalOrderResponseDto create(RentalOrderRequestDto request);
    RentalOrderResponseDto findById(Long id);
    List<RentalOrderResponseDto> findAll();
    Page<RentalOrderResponseDto> search(
            String orderStatus,
            Long customerId,
            String shippingAddress,
            BigDecimal minTotalPrice,
            BigDecimal maxTotalPrice,
            BigDecimal minPricePerDay,
            BigDecimal maxPricePerDay,
            String startDateFrom,
            String startDateTo,
            String endDateFrom,
            String endDateTo,
            String createdAtFrom,
            String createdAtTo,
            Pageable pageable);
    RentalOrderResponseDto update(Long id, RentalOrderRequestDto request);
    void delete(Long id);
    RentalOrderResponseDto confirmReturn(Long id);
    RentalOrderResponseDto extend(RentalOrderExtendRequestDto request);
}
