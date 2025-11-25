package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.model.Device;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.staff.model.HandoverReportItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverReportItemResponseDto {
    private Long deviceId;
    private String deviceSerialNumber;
    private String deviceModelName;
    private List<String> evidenceUrls;

    public static HandoverReportItemResponseDto fromEntity(HandoverReportItem item) {
        if (item == null) {
            return null;
        }
        Allocation allocation = item.getAllocation();
        Device device = allocation != null ? allocation.getDevice() : null;
        DeviceModel model = device != null ? device.getDeviceModel() : null;
        return HandoverReportItemResponseDto.builder()
                .deviceId(device != null ? device.getDeviceId() : null)
                .deviceSerialNumber(device != null ? device.getSerialNumber() : null)
                .deviceModelName(model != null ? model.getDeviceName() : null)
                .evidenceUrls(item.getEvidenceUrls() == null ? List.of() : List.copyOf(item.getEvidenceUrls()))
                .build();
    }
}
