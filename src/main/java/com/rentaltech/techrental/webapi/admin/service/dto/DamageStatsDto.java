package com.rentaltech.techrental.webapi.admin.service.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class DamageStatsDto {
    int year;
    int month;
    BigDecimal discrepancyPenaltyTotal;
    BigDecimal settlementDamageFeeTotal;
    BigDecimal totalDamage;
}


