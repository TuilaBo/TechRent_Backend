package com.rentaltech.techrental.maintenance.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriorityMaintenanceDeviceDto {
    private Long deviceId;
    private String deviceSerialNumber;
    private String deviceModelName;
    private String deviceCategoryName;
    private Integer currentUsageCount;
    private Integer requiredUsageCount;
    private LocalDate nextMaintenanceDate;
    private String priorityReason; // "USAGE_THRESHOLD", "SCHEDULED_MAINTENANCE"
    private Long maintenanceScheduleId;
}

