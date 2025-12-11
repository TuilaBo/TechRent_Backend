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
    private LocalDateTime extensionStartDate;
    private LocalDateTime extensionEndDate;
    private Integer extensionDays;
    private BigDecimal totalPrice;
    private BigDecimal depositAmount;
    private RentalOrderExtensionStatus status;
    private LocalDateTime createdAt;

    public static RentalOrderExtensionResponseDto from(RentalOrderExtension extension) {
        if (extension == null) {
            return null;
        }
        return RentalOrderExtensionResponseDto.builder()
                .extensionId(extension.getExtensionId())
                .extensionStartDate(extension.getExtensionStartDate())
                .extensionEndDate(extension.getExtensionEndDate())
                .extensionDays(extension.getExtensionDays())
                .totalPrice(extension.getTotalPrice())
                .depositAmount(extension.getDepositAmount())
                .status(extension.getStatus())
                .createdAt(extension.getCreatedAt())
                .build();
    }
}
