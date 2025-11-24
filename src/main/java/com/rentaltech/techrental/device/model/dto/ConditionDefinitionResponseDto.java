package com.rentaltech.techrental.device.model.dto;

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
}
