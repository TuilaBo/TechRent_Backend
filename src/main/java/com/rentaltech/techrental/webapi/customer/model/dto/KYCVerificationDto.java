package com.rentaltech.techrental.webapi.customer.model.dto;

import com.rentaltech.techrental.webapi.customer.model.KYCStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KYCVerificationDto {
    private KYCStatus status; // VERIFIED hoặc REJECTED
    private String rejectionReason; // Lý do từ chối nếu REJECTED
    private LocalDateTime verifiedAt;
    private Long verifiedBy; // ID operator
}


