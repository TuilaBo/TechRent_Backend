package com.rentaltech.techrental.webapi.customer.model.dto;

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
}

