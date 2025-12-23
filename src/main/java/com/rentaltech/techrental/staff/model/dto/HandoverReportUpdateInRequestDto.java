package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.device.model.dto.DiscrepancyInlineRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
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
    @NotNull
    private List<@Valid DiscrepancyInlineRequestDto> discrepancies = new ArrayList<>();
}
