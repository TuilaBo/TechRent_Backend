package com.rentaltech.techrental.contract.model.dto;

import com.rentaltech.techrental.contract.model.DeviceContractTerm;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class DeviceContractTermResponseDto {

    Long deviceContractTermId;
    String title;
    String content;
    boolean active;
    Long deviceId;
    String deviceSerialNumber;
    Long deviceCategoryId;
    String deviceCategoryName;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static DeviceContractTermResponseDto from(DeviceContractTerm term) {
        return DeviceContractTermResponseDto.builder()
                .deviceContractTermId(term.getDeviceContractTermId())
                .title(term.getTitle())
                .content(term.getContent())
                .active(Boolean.TRUE.equals(term.getActive()))
                .deviceId(term.getDevice() != null ? term.getDevice().getDeviceId() : null)
                .deviceSerialNumber(term.getDevice() != null ? term.getDevice().getSerialNumber() : null)
                .deviceCategoryId(term.getDeviceCategory() != null ? term.getDeviceCategory().getDeviceCategoryId() : null)
                .deviceCategoryName(term.getDeviceCategory() != null ? term.getDeviceCategory().getDeviceCategoryName() : null)
                .createdAt(term.getCreatedAt())
                .updatedAt(term.getUpdatedAt())
                .build();
    }
}

