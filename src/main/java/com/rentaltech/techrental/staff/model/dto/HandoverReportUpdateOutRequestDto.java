package com.rentaltech.techrental.staff.model.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverReportUpdateOutRequestDto extends HandoverReportBaseUpdateRequestDto {

    @Builder.Default
    private List<@Valid HandoverDeviceConditionRequestDto> deviceConditions = new ArrayList<>();
}
