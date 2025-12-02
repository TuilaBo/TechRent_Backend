package com.rentaltech.techrental.device.model.dto;

import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponseDto {
    private Long deviceId;
    private String serialNumber;
    private LocalDateTime acquireAt;
    private DeviceStatus status;
    private String shelfCode;
    private Long deviceModelId;
    private List<DeviceConditionResponseDto> currentConditions;

    public static DeviceResponseDto from(Device entity) {
        return from(entity, List.of());
    }

    public static DeviceResponseDto from(Device entity, List<DeviceConditionResponseDto> conditions) {
        if (entity == null) {
            return null;
        }
        var model = entity.getDeviceModel();
        return DeviceResponseDto.builder()
                .deviceId(entity.getDeviceId())
                .serialNumber(entity.getSerialNumber())
                .acquireAt(entity.getAcquireAt())
                .status(entity.getStatus())
                .shelfCode(null)
                .deviceModelId(model != null ? model.getDeviceModelId() : null)
                .currentConditions(conditions == null ? new ArrayList<>() : new ArrayList<>(conditions))
                .build();
    }
}
