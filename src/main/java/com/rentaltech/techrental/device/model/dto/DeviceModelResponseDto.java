package com.rentaltech.techrental.device.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceModelResponseDto {
    private Long deviceModelId;
    private String deviceName;
    private Long brandId;
    private String imageURL;
    private String specifications;
    private boolean isActive;
    private Long deviceCategoryId;

    // Pricing fields
    private Double deviceValue;
    private Double pricePerDay;
    private Double depositPercent;
}
