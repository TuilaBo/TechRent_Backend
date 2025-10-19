package com.rentaltech.techrental.common.dto;

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
public class AuthErrorResponseDto {
    private String error;
    private String message;
    private String details;
    private int status;
}
