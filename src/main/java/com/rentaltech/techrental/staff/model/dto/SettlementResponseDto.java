package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.Settlement;
import com.rentaltech.techrental.staff.model.SettlementState;
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
public class SettlementResponseDto {
    private Long settlementId;
    private Long orderId;
    private BigDecimal totalDeposit;
    private BigDecimal damageFee;
    private BigDecimal lateFee;
    private BigDecimal accessoryFee;
    private BigDecimal finalReturnAmount;
    private SettlementState state;
    private LocalDateTime issuedAt;

    public static SettlementResponseDto from(Settlement settlement) {
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
                .accessoryFee(settlement.getAccessoryFee())
                .finalReturnAmount(settlement.getFinalReturnAmount())
                .state(settlement.getState())
                .issuedAt(settlement.getIssuedAt())
                .build();
    }
}

