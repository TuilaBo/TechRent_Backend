package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.HandoverReport;
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
    private List<HandoverReportItemDto> items;
    private List<String> evidenceUrls;
    private List<HandoverReportStaffDto> technicians;

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
                .items(report.getItems() == null ? List.of() :
                        report.getItems().stream()
                                .map(HandoverReportItemDto::fromEntity)
                                .collect(Collectors.toList()))
                .evidenceUrls(report.getEvidenceUrls() == null ? List.of() : List.copyOf(report.getEvidenceUrls()))
                .technicians(report.getTask() == null || report.getTask().getAssignedStaff() == null
                        ? List.of()
                        : report.getTask().getAssignedStaff().stream()
                                .map(HandoverReportStaffDto::fromEntity)
                                .collect(Collectors.toList()))
                .build();
    }
}

