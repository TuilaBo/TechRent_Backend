package com.rentaltech.techrental.webapi.customer.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

