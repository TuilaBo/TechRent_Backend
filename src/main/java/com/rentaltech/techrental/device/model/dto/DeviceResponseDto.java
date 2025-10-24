package com.rentaltech.techrental.device.model.dto;

import com.rentaltech.techrental.device.model.DeviceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
}
