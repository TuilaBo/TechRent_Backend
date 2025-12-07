package com.rentaltech.techrental.webapi.customer.model.dto;

import com.rentaltech.techrental.webapi.customer.model.ComplaintStatus;
import com.rentaltech.techrental.webapi.customer.model.CustomerComplaint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerComplaintResponseDto {

    private Long complaintId;
    private Long orderId;
    private Long deviceId;
    private String deviceSerialNumber;
    private String deviceModelName;
    private Long allocationId;
    private ComplaintStatus status;
    private String customerDescription;
    private String staffNote;
    private Long replacementDeviceId;
    private String replacementDeviceSerialNumber;
    private Long replacementTaskId;
    private Long replacementAllocationId;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private LocalDateTime resolvedAt;
    private Long resolvedByStaffId;
    private String resolvedByStaffName;

    public static CustomerComplaintResponseDto from(CustomerComplaint complaint) {
        if (complaint == null) {
            return null;
        }

        return CustomerComplaintResponseDto.builder()
                .complaintId(complaint.getComplaintId())
                .orderId(complaint.getRentalOrder() != null ? complaint.getRentalOrder().getOrderId() : null)
                .deviceId(complaint.getDevice() != null ? complaint.getDevice().getDeviceId() : null)
                .deviceSerialNumber(complaint.getDevice() != null ? complaint.getDevice().getSerialNumber() : null)
                .deviceModelName(complaint.getDevice() != null && complaint.getDevice().getDeviceModel() != null
                        ? complaint.getDevice().getDeviceModel().getDeviceName() : null)
                .allocationId(complaint.getAllocation() != null ? complaint.getAllocation().getAllocationId() : null)
                .status(complaint.getStatus())
                .customerDescription(complaint.getCustomerDescription())
                .staffNote(complaint.getStaffNote())
                .replacementDeviceId(complaint.getReplacementDevice() != null 
                        ? complaint.getReplacementDevice().getDeviceId() : null)
                .replacementDeviceSerialNumber(complaint.getReplacementDevice() != null 
                        ? complaint.getReplacementDevice().getSerialNumber() : null)
                .replacementTaskId(complaint.getReplacementTask() != null 
                        ? complaint.getReplacementTask().getTaskId() : null)
                .replacementAllocationId(complaint.getReplacementAllocation() != null 
                        ? complaint.getReplacementAllocation().getAllocationId() : null)
                .createdAt(complaint.getCreatedAt())
                .processedAt(complaint.getProcessedAt())
                .resolvedAt(complaint.getResolvedAt())
                .resolvedByStaffId(complaint.getResolvedBy() != null ? complaint.getResolvedBy().getStaffId() : null)
                .resolvedByStaffName(complaint.getResolvedBy() != null && complaint.getResolvedBy().getAccount() != null
                        ? complaint.getResolvedBy().getAccount().getUsername() : null)
                .build();
    }
}

