package com.rentaltech.techrental.rentalorder.model.dto;

import com.rentaltech.techrental.rentalorder.model.RentalOrderExtension;
import com.rentaltech.techrental.rentalorder.model.RentalOrderExtensionStatus;
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
public class RentalOrderExtensionResponseDto {
    private Long extensionId;
    private LocalDateTime extensionStart;
    private LocalDateTime extensionEnd;
    private Integer durationDays;
    private BigDecimal pricePerDay;
    private BigDecimal additionalPrice;
    private LocalDateTime createdAt;
    private RentalOrderExtensionStatus status;

    public static RentalOrderExtensionResponseDto from(RentalOrderExtension extension) {
        if (extension == null) {
            return null;
        }
        return RentalOrderExtensionResponseDto.builder()
                .extensionId(extension.getExtensionId())
                .extensionStart(extension.getExtensionStart())
                .extensionEnd(extension.getExtensionEnd())
                .durationDays(extension.getDurationDays())
                .pricePerDay(extension.getPricePerDay())
                .additionalPrice(extension.getAdditionalPrice())
                .createdAt(extension.getCreatedAt())
                .status(extension.getStatus())
                .build();
    }
}
