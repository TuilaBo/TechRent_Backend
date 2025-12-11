package com.rentaltech.techrental.rentalorder.service;

import com.rentaltech.techrental.rentalorder.model.LateFeeConfig;
import com.rentaltech.techrental.rentalorder.model.dto.LateFeeConfigRequestDto;
import com.rentaltech.techrental.rentalorder.model.dto.LateFeeConfigResponseDto;
import com.rentaltech.techrental.rentalorder.repository.LateFeeConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class LateFeeConfigServiceImpl implements LateFeeConfigService {

    private final LateFeeConfigRepository repository;
    private static final BigDecimal DEFAULT_LATE_FEE = BigDecimal.ZERO;

    @Override
    @Transactional(readOnly = true)
    public LateFeeConfigResponseDto getCurrentConfig() {
        return LateFeeConfigResponseDto.from(resolveConfig());
    }

    @Override
    public LateFeeConfigResponseDto update(LateFeeConfigRequestDto request) {
        if (request == null || request.getHourlyFee() == null) {
            throw new IllegalArgumentException("Mức phí trả trễ không được để trống");
        }
        if (request.getHourlyFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Mức phí trả trễ phải lớn hơn hoặc bằng 0");
        }
        LateFeeConfig config = resolveConfig();
        config.setHourlyFee(request.getHourlyFee());
        LateFeeConfig saved = repository.save(config);
        return LateFeeConfigResponseDto.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getHourlyFeeValue() {
        return resolveConfig().getHourlyFee();
    }

    private LateFeeConfig resolveConfig() {
        return repository.findTopByOrderByIdAsc()
                .orElseGet(this::createDefaultConfig);
    }

    private LateFeeConfig createDefaultConfig() {
        LateFeeConfig config = LateFeeConfig.builder()
                .hourlyFee(DEFAULT_LATE_FEE)
                .build();
        return repository.save(config);
    }
}
