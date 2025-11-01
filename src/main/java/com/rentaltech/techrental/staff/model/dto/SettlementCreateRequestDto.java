package com.rentaltech.techrental.staff.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementCreateRequestDto {
    @NotNull
    private Long orderId;
    
    @NotNull
    private BigDecimal totalRent;
    
    private BigDecimal damageFee;
    
    private BigDecimal lateFee;
    
    private BigDecimal accessoryFee;
    
    private BigDecimal depositUsed;
    
    @NotNull
    private BigDecimal finalAmount;
}

