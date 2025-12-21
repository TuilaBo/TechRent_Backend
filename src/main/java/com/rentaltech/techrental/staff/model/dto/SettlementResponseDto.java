package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.device.model.dto.DiscrepancyReportResponseDto;
import com.rentaltech.techrental.staff.model.Settlement;
import com.rentaltech.techrental.staff.model.SettlementState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponseDto {
    private Long settlementId;
    private Long orderId;
    private BigDecimal totalDeposit;
    private BigDecimal damageFee;
    private BigDecimal lateFee;
    private BigDecimal finalReturnAmount;
    private SettlementState state;
    private LocalDateTime issuedAt;
    private List<DiscrepancyReportResponseDto> damageDetails; // Chi tiết từng hư hỏng và phí

    public static SettlementResponseDto from(Settlement settlement) {
        return from(settlement, null);
    }

    public static SettlementResponseDto from(Settlement settlement, List<DiscrepancyReportResponseDto> damageDetails) {
        if (settlement == null) {
            return null;
        }
        var order = settlement.getRentalOrder();
        return SettlementResponseDto.builder()
                .settlementId(settlement.getSettlementId())
                .orderId(order != null ? order.getOrderId() : null)
                .totalDeposit(settlement.getTotalDeposit())
                .damageFee(settlement.getDamageFee())
                .lateFee(settlement.getLateFee())
                .finalReturnAmount(settlement.getFinalReturnAmount())
                .state(settlement.getState())
                .issuedAt(settlement.getIssuedAt())
                .damageDetails(damageDetails != null ? damageDetails : Collections.emptyList())
                .build();
    }
}

