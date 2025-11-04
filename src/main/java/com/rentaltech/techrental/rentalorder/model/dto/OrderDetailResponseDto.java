package com.rentaltech.techrental.rentalorder.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponseDto {
    private Long orderDetailId;
    private Long quantity;
    private BigDecimal pricePerDay;
    private BigDecimal depositAmountPerUnit;
    private Long deviceModelId;
}
