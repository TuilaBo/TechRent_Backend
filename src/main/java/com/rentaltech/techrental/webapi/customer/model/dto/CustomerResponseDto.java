package com.rentaltech.techrental.webapi.customer.model.dto;

import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.CustomerStatus;
import com.rentaltech.techrental.webapi.customer.model.KYCStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponseDto {
    private Long customerId;
    private Long accountId;
    private String username;
    private String email;
    private String phoneNumber;
    private String fullName;
    private KYCStatus kycStatus;
    private List<ShippingAddressResponseDto> shippingAddressDtos;
    private List<BankInformationResponseDto> bankInformationDtos;
    private CustomerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CustomerResponseDto from(Customer customer) {
        if (customer == null) {
            return null;
        }
        var account = customer.getAccount();
        List<ShippingAddressResponseDto> shippingDtos = customer.getShippingAddresses() == null
                ? List.of()
                : customer.getShippingAddresses().stream()
                .filter(Objects::nonNull)
                .map(ShippingAddressResponseDto::from)
                .toList();
        List<BankInformationResponseDto> bankDtos = customer.getBankInformations() == null
                ? List.of()
                : customer.getBankInformations().stream()
                .filter(Objects::nonNull)
                .map(BankInformationResponseDto::from)
                .toList();
        return CustomerResponseDto.builder()
                .customerId(customer.getCustomerId())
                .accountId(account != null ? account.getAccountId() : null)
                .username(account != null ? account.getUsername() : null)
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .fullName(customer.getFullName())
                .kycStatus(customer.getKycStatus())
                .shippingAddressDtos(shippingDtos)
                .bankInformationDtos(bankDtos)
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
