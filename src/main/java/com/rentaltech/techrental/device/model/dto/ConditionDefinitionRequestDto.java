package com.rentaltech.techrental.device.model.dto;

import com.rentaltech.techrental.device.model.ConditionSeverity;
import com.rentaltech.techrental.device.model.ConditionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotNull
    private ConditionType conditionType;

    @NotNull
    private ConditionSeverity conditionSeverity;

    @DecimalMin("0.0")
    private BigDecimal defaultCompensation;
}
