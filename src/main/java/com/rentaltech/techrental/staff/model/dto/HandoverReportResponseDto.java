package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.HandoverReport;
import com.rentaltech.techrental.staff.model.HandoverReportStatus;
import com.rentaltech.techrental.staff.model.HandoverType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverReportResponseDto {

    private Long handoverReportId;
    private Long taskId;
    private Long orderId;
    private String customerInfo;
    private String technicianInfo;
    private LocalDateTime handoverDateTime;
    private String handoverLocation;
    private String customerSignature;
    private HandoverReportStatus status;
    private Boolean staffSigned;
    private LocalDateTime staffSignedAt;
    private String staffSignature;
    private Boolean customerSigned;
    private LocalDateTime customerSignedAt;
    private LocalDateTime deliveryDateTime;
    private HandoverType handoverType;
    private List<HandoverReportStaffDto> deliveryStaff;
    private List<HandoverReportItemResponseDto> items;
    private List<HandoverReportStaffDto> technicians;
    private List<HandoverDeviceConditionResponseDto> deviceConditions;
    private HandoverReportStaffDto createdByStaff;

    public static HandoverReportResponseDto fromEntity(HandoverReport report) {
        if (report == null) {
            return null;
        }
        return HandoverReportResponseDto.builder()
                .handoverReportId(report.getHandoverReportId())
                .taskId(report.getTask() != null ? report.getTask().getTaskId() : null)
                .orderId(report.getRentalOrder() != null ? report.getRentalOrder().getOrderId() : null)
                .customerInfo(report.getCustomerInfo())
                .technicianInfo(report.getTechnicianInfo())
                .handoverDateTime(report.getHandoverDateTime())
                .handoverLocation(report.getHandoverLocation())
                .customerSignature(report.getCustomerSignature())
                .status(report.getStatus())
                .staffSigned(report.getStaffSigned())
                .staffSignedAt(report.getStaffSignedAt())
                .staffSignature(report.getStaffSignature())
                .customerSigned(report.getCustomerSigned())
                .customerSignedAt(report.getCustomerSignedAt())
                .deliveryDateTime(report.getTask() != null && report.getTask().getCompletedAt() != null
                        ? report.getTask().getCompletedAt()
                        : report.getHandoverDateTime())
                .deliveryStaff(report.getTask() != null && report.getTask().getAssignedStaff() != null
                        ? report.getTask().getAssignedStaff().stream()
                                .map(HandoverReportStaffDto::fromEntity)
                                .collect(Collectors.toList())
                        : List.of())
                .handoverType(report.getHandoverType())
                .items(report.getItems() == null ? List.of() :
                        report.getItems().stream()
                                .map(HandoverReportItemResponseDto::fromEntity)
                                .collect(Collectors.toList()))
                .technicians(report.getTask() == null || report.getTask().getAssignedStaff() == null
                        ? List.of()
                        : report.getTask().getAssignedStaff().stream()
                                .map(HandoverReportStaffDto::fromEntity)
                                .collect(Collectors.toList()))
                .deviceConditions(HandoverDeviceConditionResponseDto.fromReport(report))
                .createdByStaff(report.getCreatedByStaff() != null
                        ? HandoverReportStaffDto.fromEntity(report.getCreatedByStaff())
                        : null)
                .build();
    }
}

