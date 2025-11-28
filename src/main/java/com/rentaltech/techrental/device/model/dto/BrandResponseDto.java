package com.rentaltech.techrental.device.model.dto;

import com.rentaltech.techrental.device.model.Brand;
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

    public static BrandResponseDto from(Brand entity) {
        if (entity == null) {
            return null;
        }
        return BrandResponseDto.builder()
                .brandId(entity.getBrandId())
                .brandName(entity.getBrandName())
                .description(entity.getDescription())
                .isActive(entity.isActive())
                .build();
    }
}

