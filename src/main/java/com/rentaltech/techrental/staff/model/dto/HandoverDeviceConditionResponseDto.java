package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.HandoverReport;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@Builder
public class HandoverDeviceConditionResponseDto {
    private Long deviceId;
    private List<DeviceConditionSnapshotDto> baselineSnapshots;
    private List<DeviceConditionSnapshotDto> finalSnapshots;

    public static List<HandoverDeviceConditionResponseDto> fromReport(HandoverReport report) {
        if (report == null || report.getItems() == null) {
            return List.of();
        }
        return report.getItems().stream()
                .map(item -> item.getAllocation())
                .filter(Objects::nonNull)
                .map(allocation -> HandoverDeviceConditionResponseDto.builder()
                        .deviceId(allocation.getDevice() != null ? allocation.getDevice().getDeviceId() : null)
                        .baselineSnapshots(allocation.getBaselineSnapshots() == null ? List.of()
                                : allocation.getBaselineSnapshots().stream()
                                        .map(DeviceConditionSnapshotDto::fromEntity)
                                        .collect(Collectors.toList()))
                        .finalSnapshots(allocation.getFinalSnapshots() == null ? List.of()
                                : allocation.getFinalSnapshots().stream()
                                        .map(DeviceConditionSnapshotDto::fromEntity)
                                        .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }
}
