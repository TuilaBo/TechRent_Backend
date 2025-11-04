package com.rentaltech.techrental.webapi.customer.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShippingAddressRequestDto {

    @NotBlank(message = "address is required")
    @Size(max = 500, message = "address must be <= 500 characters")
    private String address;
}

