package com.rentaltech.techrental.device.model.dto;

import com.rentaltech.techrental.device.model.Accessory;
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

    public static AccessoryResponseDto from(Accessory entity) {
        if (entity == null) {
            return null;
        }
        var category = entity.getAccessoryCategory();
        var model = entity.getDeviceModel();
        return AccessoryResponseDto.builder()
                .accessoryId(entity.getAccessoryId())
                .accessoryName(entity.getAccessoryName())
                .description(entity.getDescription())
                .imageUrl(entity.getImageUrl())
                .isActive(entity.isActive())
                .accessoryCategoryId(category != null ? category.getAccessoryCategoryId() : null)
                .deviceModelId(model != null ? model.getDeviceModelId() : null)
                .build();
    }
}
