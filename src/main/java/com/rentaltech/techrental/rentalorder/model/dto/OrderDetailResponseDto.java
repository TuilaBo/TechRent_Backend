package com.rentaltech.techrental.rentalorder.model.dto;

import com.rentaltech.techrental.rentalorder.model.OrderDetail;
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

    public static OrderDetailResponseDto from(OrderDetail detail) {
        if (detail == null) {
            return null;
        }
        var model = detail.getDeviceModel();
        return OrderDetailResponseDto.builder()
                .orderDetailId(detail.getOrderDetailId())
                .quantity(detail.getQuantity())
                .pricePerDay(detail.getPricePerDay())
                .depositAmountPerUnit(detail.getDepositAmountPerUnit())
                .deviceModelId(model != null ? model.getDeviceModelId() : null)
                .build();
    }
}
