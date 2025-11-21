package com.rentaltech.techrental.maintenance.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceConflictResponseDto {
    private Long deviceId;
    private String deviceSerialNumber;
    private String deviceModelName;
    private List<ConflictInfo> conflicts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictInfo {
        private Long scheduleId;
        private LocalDate scheduleStartDate;
        private LocalDate scheduleEndDate;
        private String scheduleStatus;
    }
}

