package com.rentaltech.techrental.webapi.admin.service.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class RevenueStatsDto {
    int year;
    Integer month; // null nếu query theo ngày
    Integer day; // null nếu query theo tháng
    
    // Tiền thuê (từ Invoice RENT_PAYMENT)
    BigDecimal rentalRevenue;
    
    // Tiền phạt trả muộn (từ Settlement)
    BigDecimal lateFeeRevenue;
    
    // Tiền bồi thường thiệt hại (từ Settlement)
    BigDecimal damageFeeRevenue;
    
    // Tiền cọc trả lại cho khách (từ Invoice DEPOSIT_REFUND)
    BigDecimal depositRefund;
    
    // Tổng doanh thu = rental + lateFee + damageFee - depositRefund
    BigDecimal totalRevenue;
}
