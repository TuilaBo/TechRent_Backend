package com.rentaltech.techrental.device.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationConditionDetail {
    private Long conditionDefinitionId;
    private String severity;
}
