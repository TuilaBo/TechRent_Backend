package com.rentaltech.techrental.device.model.dto;

import com.rentaltech.techrental.device.model.DeviceCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCategoryResponseDto {
    private Long deviceCategoryId;
    private String deviceCategoryName;
    private String description;
    private boolean isActive;

    public static DeviceCategoryResponseDto from(DeviceCategory entity) {
        if (entity == null) {
            return null;
        }
        return DeviceCategoryResponseDto.builder()
                .deviceCategoryId(entity.getDeviceCategoryId())
                .deviceCategoryName(entity.getDeviceCategoryName())
                .description(entity.getDescription())
                .isActive(entity.isActive())
                .build();
    }
}
