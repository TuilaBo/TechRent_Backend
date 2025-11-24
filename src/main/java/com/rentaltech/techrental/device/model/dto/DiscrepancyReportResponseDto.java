package com.rentaltech.techrental.device.model.dto;

import com.rentaltech.techrental.device.model.DiscrepancyCreatedFrom;
import com.rentaltech.techrental.device.model.DiscrepancyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscrepancyReportResponseDto {
    private Long discrepancyReportId;
    private DiscrepancyCreatedFrom createdFrom;
    private Long refId;
    private DiscrepancyType discrepancyType;
    private Long conditionDefinitionId;
    private String conditionName;
    private Long allocationId;
    private BigDecimal penaltyAmount;
    private String staffNote;
    private String customerNote;
    private LocalDateTime createdAt;
}
