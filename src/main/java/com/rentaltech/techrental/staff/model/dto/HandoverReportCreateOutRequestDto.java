package com.rentaltech.techrental.staff.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class HandoverReportCreateOutRequestDto extends HandoverReportBaseCreateRequestDto {

    @Builder.Default
    @NotNull
    private List<@Valid HandoverDeviceConditionRequestDto> deviceConditions = new ArrayList<>();
}
