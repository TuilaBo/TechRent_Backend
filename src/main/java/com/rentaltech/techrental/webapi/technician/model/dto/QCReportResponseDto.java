package com.rentaltech.techrental.webapi.technician.model.dto;

import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.dto.DeviceResponseDto;
import com.rentaltech.techrental.device.model.DiscrepancyReport;
import com.rentaltech.techrental.device.model.dto.DiscrepancyReportResponseDto;
import com.rentaltech.techrental.webapi.technician.model.QCPhase;
import com.rentaltech.techrental.webapi.technician.model.QCReport;
import com.rentaltech.techrental.webapi.technician.model.QCResult;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.staff.model.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QCReportResponseDto {
    private Long qcReportId;
    private QCPhase phase;
    private QCResult result;
    private String findings;
    private String accessorySnapShotUrl;
    private LocalDateTime createdAt;
    private String createdBy;
    private Long taskId;
    private Long orderDetailId;
    private Long orderId;
    private List<DeviceResponseDto> devices;
    private List<QCReportDeviceConditionResponseDto> deviceConditions;
    private List<DiscrepancyReportResponseDto> discrepancies;

    public static QCReportResponseDto from(QCReport report,
                                           List<Allocation> allocations,
                                           List<DiscrepancyReport> discrepancies) {
        if (report == null) {
            return null;
        }
        List<Allocation> safeAllocations = allocations == null
                ? List.of()
                : allocations.stream()
                .filter(Objects::nonNull)
                .toList();
        OrderDetail orderDetail = safeAllocations.stream()
                .map(Allocation::getOrderDetail)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        Long orderDetailId = orderDetail != null ? orderDetail.getOrderDetailId() : null;
        Long orderId = Optional.ofNullable(orderDetail)
                .map(OrderDetail::getRentalOrder)
                .map(RentalOrder::getOrderId)
                .orElseGet(() -> {
                    RentalOrder rentalOrder = report.getRentalOrder();
                    if (rentalOrder != null && rentalOrder.getOrderId() != null) {
                        return rentalOrder.getOrderId();
                    }
                    Task task = report.getTask();
                    return task != null ? task.getOrderId() : null;
                });
        List<DeviceResponseDto> devices = safeAllocations.stream()
                .map(Allocation::getDevice)
                .filter(Objects::nonNull)
                .map(DeviceResponseDto::from)
                .collect(Collectors.toList());
        List<QCReportDeviceConditionResponseDto> deviceConditions = safeAllocations.stream()
                .map(QCReportDeviceConditionResponseDto::fromAllocation)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<DiscrepancyReportResponseDto> discrepancyDtos = discrepancies == null
                ? List.of()
                : discrepancies.stream()
                .filter(Objects::nonNull)
                .map(DiscrepancyReportResponseDto::from)
                .collect(Collectors.toList());
        var task = report.getTask();
        return QCReportResponseDto.builder()
                .qcReportId(report.getQcReportId())
                .phase(report.getPhase())
                .result(report.getResult())
                .findings(report.getFindings())
                .accessorySnapShotUrl(report.getAccessorySnapShotUrl())
                .createdAt(report.getCreatedAt())
                .createdBy(report.getCreatedBy())
                .taskId(task != null ? task.getTaskId() : null)
                .orderDetailId(orderDetailId)
                .orderId(orderId)
                .devices(devices)
                .deviceConditions(deviceConditions)
                .discrepancies(discrepancyDtos)
                .build();
    }
}
