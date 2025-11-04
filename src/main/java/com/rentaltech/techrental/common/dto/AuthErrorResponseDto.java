package com.rentaltech.techrental.common.dto;

import lombok.*;

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
