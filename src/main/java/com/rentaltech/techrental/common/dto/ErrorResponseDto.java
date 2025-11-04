package com.rentaltech.techrental.common.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponseDto {
    private String error;
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private String path;
}
