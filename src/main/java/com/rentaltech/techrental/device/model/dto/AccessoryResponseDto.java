package com.rentaltech.techrental.device.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessoryResponseDto {
    private Long accessoryId;
    private String accessoryName;
    private String description;
    private String imageUrl;
    private boolean isActive;
    private Long accessoryCategoryId;
    private Long deviceModelId;
}
