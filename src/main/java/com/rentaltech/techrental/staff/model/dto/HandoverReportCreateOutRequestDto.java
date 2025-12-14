package com.rentaltech.techrental.staff.model.dto;

import jakarta.validation.Valid;
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
    private List<@Valid HandoverDeviceConditionRequestDto> deviceConditions = new ArrayList<>();
}
