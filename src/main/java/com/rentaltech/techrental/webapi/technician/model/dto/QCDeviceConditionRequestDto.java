package com.rentaltech.techrental.webapi.technician.model.dto;

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
public class QCDeviceConditionRequestDto {

    @NotNull
    private Long deviceId;

    @NotNull
    private Long conditionDefinitionId;

    private String severity;

    @Builder.Default
    private List<String> images = new ArrayList<>();
}
