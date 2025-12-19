/*package com.rentaltech.techrental.device.model.dto;

import com.rentaltech.techrental.device.model.AccessoryCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessoryCategoryResponseDto {
    private Long accessoryCategoryId;
    private String accessoryCategoryName;
    private String description;
    private boolean isActive;

    public static AccessoryCategoryResponseDto from(AccessoryCategory entity) {
        if (entity == null) {
            return null;
        }
        return AccessoryCategoryResponseDto.builder()
                .accessoryCategoryId(entity.getAccessoryCategoryId())
                .accessoryCategoryName(entity.getAccessoryCategoryName())
                .description(entity.getDescription())
                .isActive(entity.isActive())
                .build();
    }
}
*/
