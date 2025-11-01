package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.SettlementState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementUpdateRequestDto {
    private BigDecimal totalRent;
    private BigDecimal damageFee;
    private BigDecimal lateFee;
    private BigDecimal accessoryFee;
    private BigDecimal depositUsed;
    private BigDecimal finalAmount;
    private SettlementState state;
}

