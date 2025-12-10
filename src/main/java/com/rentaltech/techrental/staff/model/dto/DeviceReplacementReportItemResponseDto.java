package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.staff.model.DeviceReplacementReportItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceReplacementReportItemResponseDto {
    private Long itemId;
    private Long deviceId;
    private String deviceSerialNumber;
    private String deviceModelName;
    private Boolean isOldDevice; // true = device cũ, false = device mới
    private List<String> evidenceUrls;

    public static DeviceReplacementReportItemResponseDto fromEntity(DeviceReplacementReportItem item) {
        if (item == null) {
            return null;
        }
        Allocation allocation = item.getAllocation();
        Device device = allocation != null ? allocation.getDevice() : null;
        DeviceModel model = device != null ? device.getDeviceModel() : null;
        return DeviceReplacementReportItemResponseDto.builder()
                .itemId(item.getReplacementReportItemId())
                .deviceId(device != null ? device.getDeviceId() : null)
                .deviceSerialNumber(device != null ? device.getSerialNumber() : null)
                .deviceModelName(model != null ? model.getDeviceName() : null)
                .isOldDevice(item.getIsOldDevice())
                .evidenceUrls(item.getEvidenceUrls() == null ? List.of() : List.copyOf(item.getEvidenceUrls()))
                .build();
    }
}

