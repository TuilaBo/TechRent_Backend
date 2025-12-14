package com.rentaltech.techrental.rentalorder.model.dto;

import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DiscrepancyReport;
import com.rentaltech.techrental.device.model.dto.DeviceResponseDto;
import com.rentaltech.techrental.device.model.dto.DiscrepancyReportResponseDto;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.webapi.technician.model.dto.QCReportDeviceConditionResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalOrderResponseDto {
    private Long orderId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime planStartDate;
    private LocalDateTime planEndDate;
    private Integer durationDays;
    private String shippingAddress;
    private OrderStatus orderStatus;
    private BigDecimal depositAmount;
    private BigDecimal depositAmountHeld;
    private BigDecimal depositAmountUsed;
    private BigDecimal depositAmountRefunded;
    private BigDecimal totalPrice;
    private BigDecimal pricePerDay;
    private LocalDateTime createdAt;
    private Long customerId;
    private List<OrderDetailResponseDto> orderDetails;
    private List<DeviceResponseDto> allocatedDevices;
    private List<DiscrepancyReportResponseDto> discrepancies;
    private List<QCReportDeviceConditionResponseDto> deviceConditions;
    @Builder.Default
    private List<RentalOrderExtensionResponseDto> extensions = List.of();

    public static RentalOrderResponseDto from(RentalOrder order,
                                              List<OrderDetail> details,
                                              List<Device> allocatedDevices) {
        return from(order, details, allocatedDevices, List.of(), List.of());
    }

    public static RentalOrderResponseDto from(RentalOrder order,
                                              List<OrderDetail> details,
                                              List<Device> allocatedDevices,
                                              List<DiscrepancyReport> discrepancies,
                                              List<QCReportDeviceConditionResponseDto> deviceConditions) {
        if (order == null) {
            return null;
        }
        var customer = order.getCustomer();
        List<OrderDetailResponseDto> detailDtos = details == null
                ? List.of()
                : details.stream()
                .filter(Objects::nonNull)
                .map(OrderDetailResponseDto::from)
                .toList();
        List<DeviceResponseDto> allocatedDeviceDtos = allocatedDevices == null
                ? List.of()
                : allocatedDevices.stream()
                .filter(Objects::nonNull)
                .map(DeviceResponseDto::from)
                .toList();
        List<DiscrepancyReportResponseDto> discrepancyDtos = discrepancies == null
                ? List.of()
                : discrepancies.stream()
                .filter(Objects::nonNull)
                .map(DiscrepancyReportResponseDto::from)
                .toList();
        List<QCReportDeviceConditionResponseDto> deviceConditionDtos = deviceConditions == null
                ? List.of()
                : deviceConditions.stream()
                .filter(Objects::nonNull)
                .toList();
        return RentalOrderResponseDto.builder()
                .orderId(order.getOrderId())
                .startDate(order.getStartDate())
                .endDate(order.getEndDate())
                .planStartDate(order.getPlanStartDate())
                .planEndDate(order.getPlanEndDate())
                .durationDays(order.getDurationDays())
                .shippingAddress(order.getShippingAddress())
                .orderStatus(order.getOrderStatus())
                .depositAmount(order.getDepositAmount())
                .depositAmountHeld(order.getDepositAmountHeld())
                .depositAmountUsed(order.getDepositAmountUsed())
                .depositAmountRefunded(order.getDepositAmountRefunded())
                .totalPrice(order.getTotalPrice())
                .pricePerDay(order.getPricePerDay())
                .createdAt(order.getCreatedAt())
                .customerId(customer != null ? customer.getCustomerId() : null)
                .orderDetails(detailDtos)
                .allocatedDevices(allocatedDeviceDtos)
                .discrepancies(discrepancyDtos)
                .deviceConditions(deviceConditionDtos)
                .extensions(List.of())
                .build();
    }
}
