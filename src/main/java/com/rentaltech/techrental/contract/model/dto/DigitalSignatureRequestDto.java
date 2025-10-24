package com.rentaltech.techrental.contract.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class DigitalSignatureRequestDto {
    
    @NotNull(message = "ID hợp đồng không được để trống")
    private Long contractId;
    
    @NotBlank(message = "Chữ ký điện tử không được để trống")
    private String digitalSignature; // Base64 encoded signature
    
    @NotBlank(message = "Mã PIN không được để trống")
    @Size(min = 6, max = 6, message = "Mã PIN phải có đúng 6 chữ số")
    private String pinCode; // 6-digit PIN for verification
    
    private String signatureMethod; // "DIGITAL_CERTIFICATE", "SMS_OTP", "EMAIL_OTP"
    
    private String deviceInfo; // Device fingerprint for security
    
    private String ipAddress; // IP address for audit trail
}

