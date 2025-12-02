package com.rentaltech.techrental.device.model.dto;

import com.rentaltech.techrental.device.model.ConditionType;
import com.rentaltech.techrental.device.model.ConditionSeverity;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionDefinitionRequestDto {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotNull
    private Long deviceModelId;

    @Size(max = 2000)
    private String description;

    @Digits(integer = 3, fraction = 2)
    @DecimalMin(value = "-100.00")
    @DecimalMax(value = "100.00")
    private BigDecimal impactRate;

    @NotNull
    private ConditionType conditionType;

    @NotNull
    private ConditionSeverity conditionSeverity;

    @DecimalMin("0.0")
    private BigDecimal defaultCompensation;
}
