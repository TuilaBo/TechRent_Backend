package com.rentaltech.techrental.webapi.technician.model.dto;

import com.rentaltech.techrental.device.model.dto.DiscrepancyInlineRequestDto;
import com.rentaltech.techrental.webapi.technician.model.QCResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QCReportPostRentalUpdateRequestDto {

    @NotNull
    private QCResult result;

    private String findings;

    private Map<Long, List<String>> orderDetailSerialNumbers;

    @Builder.Default
    private List<@Valid DiscrepancyInlineRequestDto> discrepancies = new ArrayList<>();
}
