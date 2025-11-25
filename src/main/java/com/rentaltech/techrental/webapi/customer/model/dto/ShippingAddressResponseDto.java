package com.rentaltech.techrental.webapi.customer.model.dto;

import com.rentaltech.techrental.webapi.customer.model.ShippingAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShippingAddressResponseDto {
    private Long shippingAddressId;
    private String address;
    private Long customerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ShippingAddressResponseDto from(ShippingAddress entity) {
        if (entity == null) {
            return null;
        }
        return ShippingAddressResponseDto.builder()
                .shippingAddressId(entity.getShippingAddressId())
                .address(entity.getAddress())
                .customerId(entity.getCustomer() != null ? entity.getCustomer().getCustomerId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

