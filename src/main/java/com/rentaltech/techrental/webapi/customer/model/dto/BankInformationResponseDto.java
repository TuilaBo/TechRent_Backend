package com.rentaltech.techrental.webapi.customer.model.dto;

import com.rentaltech.techrental.webapi.customer.model.BankInformation;
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

    public static BankInformationResponseDto from(BankInformation entity) {
        if (entity == null) {
            return null;
        }
        return BankInformationResponseDto.builder()
                .bankInformationId(entity.getBankInformationId())
                .bankName(entity.getBankName())
                .bankHolder(entity.getBankHolder())
                .cardNumber(entity.getCardNumber())
                .customerId(entity.getCustomer() != null ? entity.getCustomer().getCustomerId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

