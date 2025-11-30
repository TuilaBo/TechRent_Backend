package com.rentaltech.techrental.device.model.dto;

import jakarta.validation.Valid;
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
public class DeviceConditionUpdateRequestDto {

    private Long capturedByStaffId;

    @Builder.Default
    @Valid
    private List<DeviceConditionRequestDto> conditions = new ArrayList<>();
}
