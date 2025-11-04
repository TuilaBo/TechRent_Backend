package com.rentaltech.techrental.common.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SuccessResponseDto {
    private String status;
    private String message;
    private String details;
    private int code;
    private Object data;
}
