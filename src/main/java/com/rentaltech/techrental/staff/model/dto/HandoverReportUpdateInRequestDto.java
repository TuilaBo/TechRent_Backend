package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.device.model.dto.DiscrepancyInlineRequestDto;
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
public class HandoverReportUpdateInRequestDto extends HandoverReportBaseUpdateRequestDto {

    @Builder.Default
    private List<@Valid DiscrepancyInlineRequestDto> discrepancies = new ArrayList<>();
}
