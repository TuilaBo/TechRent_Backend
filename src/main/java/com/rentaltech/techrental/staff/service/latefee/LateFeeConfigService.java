package com.rentaltech.techrental.staff.service.latefee;

import com.rentaltech.techrental.staff.model.dto.LateFeeConfigRequestDto;
import com.rentaltech.techrental.staff.model.dto.LateFeeConfigResponseDto;

import java.math.BigDecimal;

public interface LateFeeConfigService {
    LateFeeConfigResponseDto getConfig();
    LateFeeConfigResponseDto updateConfig(LateFeeConfigRequestDto request);
    BigDecimal getCurrentRate();
}
