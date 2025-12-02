package com.rentaltech.techrental.device.model.dto;

import com.rentaltech.techrental.device.model.ConditionDefinition;
import com.rentaltech.techrental.device.model.ConditionType;
import com.rentaltech.techrental.device.model.ConditionSeverity;
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
    private Long deviceModelId;
    private String deviceModelName;
    private String description;
    private BigDecimal impactRate;
    private ConditionType conditionType;
    private ConditionSeverity conditionSeverity;
    private BigDecimal defaultCompensation;

    public static ConditionDefinitionResponseDto from(ConditionDefinition entity) {
        if (entity == null) {
            return null;
        }
        var model = entity.getDeviceModel();
        return ConditionDefinitionResponseDto.builder()
                .conditionDefinitionId(entity.getConditionDefinitionId())
                .name(entity.getName())
                .deviceModelId(model != null ? model.getDeviceModelId() : null)
                .deviceModelName(model != null ? model.getDeviceName() : null)
                .description(entity.getDescription())
                .impactRate(entity.getImpactRate())
                .conditionType(entity.getConditionType())
                .conditionSeverity(entity.getConditionSeverity())
                .defaultCompensation(entity.getDefaultCompensation())
                .build();
    }
}
