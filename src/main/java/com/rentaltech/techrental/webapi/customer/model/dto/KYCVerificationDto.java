package com.rentaltech.techrental.webapi.customer.model.dto;

import com.rentaltech.techrental.webapi.customer.model.KYCStatus;
import lombok.*;

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


