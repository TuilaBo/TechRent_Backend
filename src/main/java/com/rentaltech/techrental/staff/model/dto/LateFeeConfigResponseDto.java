package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.LateFeeConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LateFeeConfigResponseDto {
    private BigDecimal hourlyRate;
    private LocalDateTime updatedAt;

    public static LateFeeConfigResponseDto from(LateFeeConfig config) {
        if (config == null) {
            return null;
        }
        return LateFeeConfigResponseDto.builder()
                .hourlyRate(config.getHourlyRate())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
