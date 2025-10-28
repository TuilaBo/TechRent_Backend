package com.rentaltech.techrental.device.model.dto;

import lombok.*;

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

