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
public class BankInformationRequestDto {

    @NotBlank(message = "bankName is required")
    @Size(max = 100, message = "bankName must be <= 100 characters")
    private String bankName;

    @NotBlank(message = "bankHolder is required")
    @Size(max = 100, message = "bankHolder must be <= 100 characters")
    private String bankHolder;

    @NotBlank(message = "cardNumber is required")
    @Size(max = 20, message = "cardNumber must be <= 20 characters")
    private String cardNumber;
}

