package com.rentaltech.techrental.contract.model.dto;

import com.rentaltech.techrental.contract.model.Contract;
import com.rentaltech.techrental.contract.model.ContractStatus;
import com.rentaltech.techrental.contract.model.ContractType;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.dto.DeviceResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
    private Long orderId;
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
    private List<DeviceResponseDto> allocatedDevices;

    public static ContractResponseDto from(Contract contract, List<Device> allocatedDevices) {
        if (contract == null) {
            return null;
        }
        List<DeviceResponseDto> allocatedDeviceDtos = allocatedDevices == null
                ? List.of()
                : allocatedDevices.stream()
                .filter(Objects::nonNull)
                .map(DeviceResponseDto::from)
                .toList();
        return ContractResponseDto.builder()
                .contractId(contract.getContractId())
                .contractNumber(contract.getContractNumber())
                .title(contract.getTitle())
                .description(contract.getDescription())
                .contractType(contract.getContractType())
                .status(contract.getStatus())
                .customerId(contract.getCustomerId())
                .staffId(contract.getStaffId())
                .orderId(contract.getOrderId())
                .contractContent(contract.getContractContent())
                .termsAndConditions(contract.getTermsAndConditions())
                .rentalPeriodDays(contract.getRentalPeriodDays())
                .totalAmount(contract.getTotalAmount())
                .depositAmount(contract.getDepositAmount())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .signedAt(contract.getSignedAt())
                .expiresAt(contract.getExpiresAt())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .createdBy(contract.getCreatedBy())
                .updatedBy(contract.getUpdatedBy())
                .allocatedDevices(allocatedDeviceDtos)
                .build();
    }
}
