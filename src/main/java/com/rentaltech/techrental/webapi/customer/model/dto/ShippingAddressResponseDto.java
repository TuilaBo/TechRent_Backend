package com.rentaltech.techrental.webapi.customer.model.dto;

import lombok.*;

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
}

