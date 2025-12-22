package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.device.model.dto.DiscrepancyReportResponseDto;
import com.rentaltech.techrental.staff.model.DeviceReplacementReport;
import com.rentaltech.techrental.staff.model.DeviceReplacementReportStatus;
import com.rentaltech.techrental.webapi.customer.model.ComplaintFaultSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceReplacementReportResponseDto {

    private Long replacementReportId;
    private Long taskId;
    private Long orderId;
    private String customerInfo;
    private String technicianInfo;
    private LocalDateTime replacementDateTime;
    private String replacementLocation;
    private DeviceReplacementReportStatus status;
    private Boolean staffSigned;
    private LocalDateTime staffSignedAt;
    private String staffSignature;
    private Boolean customerSigned;
    private LocalDateTime customerSignedAt;
    private String customerSignature;
    private List<DeviceReplacementReportItemResponseDto> items;
    private HandoverReportStaffDto createdByStaff;
    private LocalDateTime createdAt;
    private ComplaintFaultSource faultSource; // Lỗi do ai (từ complaint)
    private List<DiscrepancyReportResponseDto> discrepancies; // Thiệt hại từ complaint

    public static DeviceReplacementReportResponseDto fromEntity(DeviceReplacementReport report) {
        return fromEntity(report, null, null);
    }

    public static DeviceReplacementReportResponseDto fromEntity(DeviceReplacementReport report,
                                                                  ComplaintFaultSource faultSource,
                                                                  List<DiscrepancyReportResponseDto> discrepancies) {
        if (report == null) {
            return null;
        }
        return DeviceReplacementReportResponseDto.builder()
                .replacementReportId(report.getReplacementReportId())
                .taskId(report.getTask() != null ? report.getTask().getTaskId() : null)
                .orderId(report.getRentalOrder() != null ? report.getRentalOrder().getOrderId() : null)
                .customerInfo(report.getCustomerInfo())
                .technicianInfo(report.getTechnicianInfo())
                .replacementDateTime(report.getReplacementDateTime())
                .replacementLocation(report.getReplacementLocation())
                .status(report.getStatus())
                .staffSigned(report.getStaffSigned())
                .staffSignedAt(report.getStaffSignedAt())
                .staffSignature(report.getStaffSignature())
                .customerSigned(report.getCustomerSigned())
                .customerSignedAt(report.getCustomerSignedAt())
                .customerSignature(report.getCustomerSignature())
                .items(report.getItems() == null ? List.of() :
                        report.getItems().stream()
                                .map(DeviceReplacementReportItemResponseDto::fromEntity)
                                .collect(Collectors.toList()))
                .createdByStaff(report.getCreatedByStaff() != null
                        ? HandoverReportStaffDto.fromEntity(report.getCreatedByStaff())
                        : null)
                .createdAt(report.getCreatedAt())
                .faultSource(faultSource)
                .discrepancies(discrepancies != null ? discrepancies : Collections.emptyList())
                .build();
    }
}

