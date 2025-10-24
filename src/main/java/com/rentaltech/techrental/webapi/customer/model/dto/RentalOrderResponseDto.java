package com.rentaltech.techrental.webapi.customer.model.dto;

import com.rentaltech.techrental.webapi.customer.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private OrderStatus orderStatus;
    private Double depositAmount;
    private Double depositAmountHeld;
    private Double depositAmountUsed;
    private Double depositAmountRefunded;
    private Double totalPrice;
    private Double pricePerDay;
    private LocalDateTime createdAt;
    private Long customerId;
    private List<OrderDetailResponseDto> orderDetails;
}
