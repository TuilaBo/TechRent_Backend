package com.rentaltech.techrental.rentalorder.model.dto;

import com.rentaltech.techrental.rentalorder.model.LateFeeConfig;
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
    private Long id;
    private BigDecimal hourlyFee;
    private LocalDateTime updatedAt;

    public static LateFeeConfigResponseDto from(LateFeeConfig config) {
        if (config == null) {
            return null;
        }
        return LateFeeConfigResponseDto.builder()
                .id(config.getId())
                .hourlyFee(config.getHourlyFee())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
