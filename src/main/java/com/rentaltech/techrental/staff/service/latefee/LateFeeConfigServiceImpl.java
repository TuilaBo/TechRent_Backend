package com.rentaltech.techrental.staff.service.latefee;

import com.rentaltech.techrental.staff.model.LateFeeConfig;
import com.rentaltech.techrental.staff.model.dto.LateFeeConfigRequestDto;
import com.rentaltech.techrental.staff.model.dto.LateFeeConfigResponseDto;
import com.rentaltech.techrental.staff.repository.LateFeeConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class LateFeeConfigServiceImpl implements LateFeeConfigService {

    private static final BigDecimal DEFAULT_RATE = new BigDecimal("0.10");

    private final LateFeeConfigRepository lateFeeConfigRepository;

    @Override
    @Transactional(readOnly = true)
    public LateFeeConfigResponseDto getConfig() {
        return lateFeeConfigRepository.findTopByOrderByIdAsc()
                .map(LateFeeConfigResponseDto::from)
                .orElse(LateFeeConfigResponseDto.builder()
                        .hourlyRate(DEFAULT_RATE)
                        .updatedAt(null)
                        .build());
    }

    @Override
    public LateFeeConfigResponseDto updateConfig(LateFeeConfigRequestDto request) {
        LateFeeConfig config = lateFeeConfigRepository.findTopByOrderByIdAsc()
                .orElse(LateFeeConfig.builder().build());
        config.setHourlyRate(request.getHourlyRate());
        config.setUpdatedAt(LocalDateTime.now());
        LateFeeConfig saved = lateFeeConfigRepository.save(config);
        return LateFeeConfigResponseDto.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentRate() {
        return lateFeeConfigRepository.findTopByOrderByIdAsc()
                .map(LateFeeConfig::getHourlyRate)
                .filter(rate -> rate != null && rate.compareTo(BigDecimal.ZERO) > 0)
                .orElse(DEFAULT_RATE);
    }
}
