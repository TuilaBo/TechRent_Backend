package com.rentaltech.techrental.device.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceModelResponseDto {
    private Long deviceModelId;
    private String deviceName;
    private String description;
    private Long brandId;
    private String imageURL;
    private String specifications;
    private boolean isActive;
    private Long deviceCategoryId;
    private Long amountAvailable;
    private BigDecimal deviceValue;
    private BigDecimal pricePerDay;
    private BigDecimal depositPercent;
}
