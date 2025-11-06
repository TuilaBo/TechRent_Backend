package com.rentaltech.techrental.webapi.customer.model.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BankInformationResponseDto {
    private Long bankInformationId;
    private String bankName;
    private String bankHolder;
    private String cardNumber;
    private Long customerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

