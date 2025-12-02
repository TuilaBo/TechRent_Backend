package com.rentaltech.techrental.device.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceConditionRequestDto {

    @NotNull
    private Long conditionDefinitionId;

    private String severity;

    private String note;

    @Builder.Default
    private List<String> images = new ArrayList<>();
}
