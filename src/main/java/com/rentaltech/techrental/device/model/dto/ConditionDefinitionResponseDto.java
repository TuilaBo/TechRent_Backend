package com.rentaltech.techrental.device.model.dto;

import com.rentaltech.techrental.device.model.ConditionDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionDefinitionResponseDto {
    private Long conditionDefinitionId;
    private String name;
    private Long deviceCategoryId;
    private String deviceCategoryName;
    private String description;
    private BigDecimal impactRate;
    private boolean damage;
    private BigDecimal defaultCompensation;

    public static ConditionDefinitionResponseDto from(ConditionDefinition entity) {
        if (entity == null) {
            return null;
        }
        var category = entity.getDeviceCategory();
        return ConditionDefinitionResponseDto.builder()
                .conditionDefinitionId(entity.getConditionDefinitionId())
                .name(entity.getName())
                .deviceCategoryId(category != null ? category.getDeviceCategoryId() : null)
                .deviceCategoryName(category != null ? category.getDeviceCategoryName() : null)
                .description(entity.getDescription())
                .impactRate(entity.getImpactRate())
                .damage(entity.isDamage())
                .defaultCompensation(entity.getDefaultCompensation())
                .build();
    }
}
