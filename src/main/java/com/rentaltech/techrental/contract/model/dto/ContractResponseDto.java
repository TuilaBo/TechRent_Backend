package com.rentaltech.techrental.contract.model.dto;

import com.rentaltech.techrental.contract.model.Contract;
import com.rentaltech.techrental.contract.model.ContractExtensionAnnex;
import com.rentaltech.techrental.contract.model.ContractStatus;
import com.rentaltech.techrental.contract.model.ContractType;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.dto.DeviceResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Thông tin chi tiết hợp đồng và các phụ lục liên quan")
public class ContractResponseDto {
    @Schema(description = "ID hợp đồng")
    private Long contractId;

    @Schema(description = "Số hợp đồng hiển thị cho người dùng")
    private String contractNumber;

    @Schema(description = "Tiêu đề hợp đồng")
    private String title;

    @Schema(description = "Mô tả ngắn gọn về hợp đồng")
    private String description;

    @Schema(description = "Loại hợp đồng")
    private ContractType contractType;

    @Schema(description = "Trạng thái hiện tại của hợp đồng")
    private ContractStatus status;

    @Schema(description = "ID khách hàng liên quan")
    private Long customerId;

    @Schema(description = "ID nhân viên phụ trách hợp đồng")
    private Long staffId;

    @Schema(description = "ID đơn thuê gốc (nếu có)")
    private Long orderId;

    @Schema(description = "Nội dung chi tiết của hợp đồng")
    private String contractContent;

    @Schema(description = "Điều khoản và điều kiện áp dụng")
    private String termsAndConditions;

    @Schema(description = "Số ngày thuê theo hợp đồng")
    private Integer rentalPeriodDays;

    @Schema(description = "Tổng tiền hợp đồng")
    private BigDecimal totalAmount;

    @Schema(description = "Số tiền đặt cọc")
    private BigDecimal depositAmount;

    @Schema(description = "Ngày bắt đầu hiệu lực")
    private LocalDateTime startDate;

    @Schema(description = "Ngày kết thúc hiệu lực")
    private LocalDateTime endDate;

    @Schema(description = "Thời điểm hợp đồng được ký")
    private LocalDateTime signedAt;

    @Schema(description = "Ngày hết hạn của hợp đồng")
    private LocalDateTime expiresAt;

    @Schema(description = "Ngày tạo bản ghi")
    private LocalDateTime createdAt;

    @Schema(description = "Ngày cập nhật bản ghi gần nhất")
    private LocalDateTime updatedAt;

    @Schema(description = "ID người tạo hợp đồng")
    private Long createdBy;

    @Schema(description = "ID người cập nhật hợp đồng gần nhất")
    private Long updatedBy;

    @Schema(description = "Tên khách hàng hiển thị")
    private String customerName;

    @Schema(description = "Tên nhân viên phụ trách")
    private String staffName;

    @Schema(description = "Tên người tạo hợp đồng")
    private String creatorName;

    @Schema(description = "Tên người cập nhật hợp đồng")
    private String updaterName;

    @Schema(description = "Đánh dấu hợp đồng đã hết hạn hay chưa")
    private boolean isExpired;

    @Schema(description = "Đánh dấu hợp đồng sắp hết hạn")
    private boolean isExpiringSoon;

    @Schema(description = "Số ngày còn lại trước khi hết hạn")
    private long daysUntilExpiry;

    @Schema(description = "Danh sách thiết bị đã phân bổ cho hợp đồng")
    private List<DeviceResponseDto> allocatedDevices;

    @Schema(description = "Danh sách phụ lục gia hạn của hợp đồng")
    private List<ContractExtensionAnnexResponseDto> extensionAnnexes;

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
        List<ContractExtensionAnnex> annexes = contract.getExtensionAnnexes();
        List<ContractExtensionAnnexResponseDto> annexDtos = annexes == null
                ? List.of()
                : annexes.stream()
                .map(ContractExtensionAnnexResponseDto::from)
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
                .extensionAnnexes(annexDtos)
                .build();
    }
}
