package com.rentaltech.techrental.rentalorder.model.dto;

import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalOrderResponseDto {
    private Long orderId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String shippingAddress;
    private OrderStatus orderStatus;
    private BigDecimal depositAmount;
    private BigDecimal depositAmountHeld;
    private BigDecimal depositAmountUsed;
    private BigDecimal depositAmountRefunded;
    private BigDecimal totalPrice;
    private BigDecimal pricePerDay;
    private LocalDateTime createdAt;
    private Long customerId;
    private List<OrderDetailResponseDto> orderDetails;
}
