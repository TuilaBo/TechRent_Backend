package com.rentaltech.techrental.contract.model.dto;

import com.rentaltech.techrental.contract.model.ContractStatus;
import com.rentaltech.techrental.contract.model.ContractType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractResponseDto {
    private Long contractId;
    private String contractNumber;
    private String title;
    private String description;
    private ContractType contractType;
    private ContractStatus status;
    private Long customerId;
    private Long staffId;
    private String contractContent;
    private String termsAndConditions;
    private Integer rentalPeriodDays;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime signedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    
    // Thông tin bổ sung
    private String customerName; // Tên khách hàng
    private String staffName;   // Tên nhân viên
    private String creatorName;  // Tên người tạo
    private String updaterName; // Tên người cập nhật
    private boolean isExpired;  // Đã hết hạn chưa
    private boolean isExpiringSoon; // Sắp hết hạn
    private long daysUntilExpiry; // Số ngày còn lại
}