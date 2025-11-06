package com.rentaltech.techrental.webapi.customer.model.dto;

import com.rentaltech.techrental.webapi.customer.model.CustomerStatus;
import com.rentaltech.techrental.webapi.customer.model.KYCStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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
}
