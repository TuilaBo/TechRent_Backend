package com.rentaltech.techrental.device.model.dto;

import com.rentaltech.techrental.device.model.DeviceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRequestDto {

    @Size(max = 100)
    private String serialNumber;

    private LocalDateTime acquireAt;

    private DeviceStatus status;

    @Size(max = 100)
    private String shelfCode;

    @NotNull
    private Long deviceModelId;
}
