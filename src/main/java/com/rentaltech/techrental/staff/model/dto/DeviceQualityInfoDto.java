package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.DeviceQualityInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceQualityInfoDto {
    private String deviceSerialNumber;
    private String qualityStatus;
    private String qualityDescription;
    private String deviceModelName;

    public DeviceQualityInfo toEntity() {
        return DeviceQualityInfo.builder()
                .deviceSerialNumber(deviceSerialNumber)
                .qualityStatus(qualityStatus)
                .qualityDescription(qualityDescription)
                .deviceModelName(deviceModelName)
                .build();
    }

    public static DeviceQualityInfoDto fromEntity(DeviceQualityInfo info) {
        if (info == null) {
            return null;
        }
        return DeviceQualityInfoDto.builder()
                .deviceSerialNumber(info.getDeviceSerialNumber())
                .qualityStatus(info.getQualityStatus())
                .qualityDescription(info.getQualityDescription())
                .deviceModelName(info.getDeviceModelName())
                .build();
    }
}

