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

    // Tiền cọc nhận vào (từ invoice RENT_PAYMENT, phần deposit)
    BigDecimal depositInbound;

    // Tiền cọc hoàn trả cho khách (invoice DEPOSIT_REFUND)
    BigDecimal depositOutbound;

    // Tổng doanh thu = rental + lateFee + damageFee
    BigDecimal totalRevenue;
}

