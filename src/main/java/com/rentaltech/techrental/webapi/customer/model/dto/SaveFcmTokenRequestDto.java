package com.rentaltech.techrental.webapi.customer.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveFcmTokenRequestDto {

    @NotBlank(message = "Token không được để trống")
    private String token;
}
