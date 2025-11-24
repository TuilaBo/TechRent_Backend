package com.rentaltech.techrental.device.model.dto;

import com.rentaltech.techrental.device.model.DiscrepancyType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscrepancyInlineRequestDto {

    @NotNull
    private DiscrepancyType discrepancyType;

    private Long conditionDefinitionId;

    @NotNull
    private Long orderDetailId;

    @NotNull
    private Long deviceId;

    @Size(max = 2000)
    private String staffNote;

    @Size(max = 2000)
    private String customerNote;
}
