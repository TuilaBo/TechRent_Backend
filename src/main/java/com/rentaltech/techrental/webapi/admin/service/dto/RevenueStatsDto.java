package com.rentaltech.techrental.webapi.admin.service.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class RevenueStatsDto {
    int year;
    Integer month; // null nếu query theo năm
    Integer day;   // null nếu query theo tháng/năm

    // Tiền thuê (chỉ totalPrice, không bao gồm cọc)
    BigDecimal rentalRevenue;

    // Tiền phạt trả muộn (từ Settlement.lateFee)
    BigDecimal lateFeeRevenue;

    // Tiền bồi thường thiệt hại (từ Settlement.damageFee)
    BigDecimal damageFeeRevenue;

    // Tổng doanh thu = rental + lateFee + damageFee
    BigDecimal totalRevenue;
}

