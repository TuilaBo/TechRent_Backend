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
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class HandoverReportCreateInRequestDto extends HandoverReportBaseCreateRequestDto {

    @Builder.Default
    @NotNull
    private List<@Valid DiscrepancyInlineRequestDto> discrepancies = new ArrayList<>();
}
