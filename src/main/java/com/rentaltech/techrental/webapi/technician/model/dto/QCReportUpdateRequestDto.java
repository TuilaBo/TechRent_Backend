package com.rentaltech.techrental.webapi.technician.model.dto;

import com.rentaltech.techrental.device.model.dto.DiscrepancyInlineRequestDto;
import com.rentaltech.techrental.webapi.technician.model.QCPhase;
import com.rentaltech.techrental.webapi.technician.model.QCResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QCReportUpdateRequestDto {
    @NotNull
    private QCPhase phase;

    @NotNull
    private QCResult result;

    private String findings;

    private Map<Long, List<String>> orderDetailSerialNumbers;

    @Builder.Default
    private List<@Valid QCDeviceConditionRequestDto> deviceConditions = new ArrayList<>();

    private List<@Valid DiscrepancyInlineRequestDto> discrepancies;
}
