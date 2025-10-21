package com.rentaltech.techrental.webapi.customer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponseDto {
    private Long orderDetailId;
    private Long quantity;
    private Double pricePerDay;
    private Double depositAmountPerUnit;
    private Long deviceModelId;
}
