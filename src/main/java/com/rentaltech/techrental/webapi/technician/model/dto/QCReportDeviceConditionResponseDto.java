package com.rentaltech.techrental.webapi.technician.model.dto;

import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.AllocationSnapshotSource;
import com.rentaltech.techrental.staff.model.dto.DeviceConditionSnapshotDto;
import lombok.Builder;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@Builder
public class QCReportDeviceConditionResponseDto {
    private Long allocationId;
    private Long deviceId;
    private String deviceSerial;
    private List<DeviceConditionSnapshotDto> snapshots;

    public static QCReportDeviceConditionResponseDto fromAllocation(Allocation allocation) {
        if (allocation == null) {
            return null;
        }
        Device device = allocation.getDevice();
        return QCReportDeviceConditionResponseDto.builder()
                .allocationId(allocation.getAllocationId())
                .deviceId(device != null ? device.getDeviceId() : null)
                .deviceSerial(device != null ? device.getSerialNumber() : null)
                .snapshots(CollectionUtils.isEmpty(allocation.getBaselineSnapshots()) ? List.of()
                        : allocation.getBaselineSnapshots().stream()
                                .filter(snapshot -> snapshot.getSource() == AllocationSnapshotSource.QC_BEFORE)
                                .map(DeviceConditionSnapshotDto::fromEntity)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()))
                .build();
    }
}
