package com.rentaltech.techrental.rentalorder.service;

import com.rentaltech.techrental.rentalorder.model.dto.LateFeeConfigRequestDto;
import com.rentaltech.techrental.rentalorder.model.dto.LateFeeConfigResponseDto;

import java.math.BigDecimal;

public interface LateFeeConfigService {
    LateFeeConfigResponseDto getCurrentConfig();
    LateFeeConfigResponseDto update(LateFeeConfigRequestDto request);
    BigDecimal getHourlyFeeValue();
}
