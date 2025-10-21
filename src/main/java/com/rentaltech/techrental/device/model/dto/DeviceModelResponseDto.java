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
    private String brand;
    private String imageURL;
    private String specifications;
    private boolean isActive;
    private Long deviceCategoryId;
}
