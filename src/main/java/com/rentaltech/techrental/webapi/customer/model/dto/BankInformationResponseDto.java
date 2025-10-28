package com.rentaltech.techrental.webapi.customer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BankInformationResponseDto {
    private Long bankInformationId;
    private String bankName;
    private String cardNumber;
    private Long customerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

