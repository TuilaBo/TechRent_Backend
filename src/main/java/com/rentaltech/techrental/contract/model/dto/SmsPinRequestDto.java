package com.rentaltech.techrental.contract.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class SmsPinRequestDto {
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9+\\-\\s()]+$", message = "Số điện thoại không đúng định dạng")
    private String phoneNumber;
}
