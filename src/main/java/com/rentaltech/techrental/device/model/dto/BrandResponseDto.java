package com.rentaltech.techrental.device.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandResponseDto {
    private Long brandId;
    private String brandName;
    private String description;
    private boolean isActive;
}

