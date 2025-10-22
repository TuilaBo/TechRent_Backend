package com.rentaltech.techrental.contract.model.dto;

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
public class DigitalSignatureResponseDto {
    private Long signatureId;
    private Long contractId;
    private String signatureHash; // SHA-256 hash of signature
    private String signatureMethod;
    private String deviceInfo;
    private String ipAddress;
    private LocalDateTime signedAt;
    private String signatureStatus; // "VALID", "INVALID", "EXPIRED"
    private String certificateInfo; // Digital certificate details
    private String auditTrail; // Complete audit information
}

