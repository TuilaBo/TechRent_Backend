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
    Long deviceModelId;
    String deviceModelName;
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
                .deviceModelId(term.getDeviceModel() != null ? term.getDeviceModel().getDeviceModelId() : null)
                .deviceModelName(term.getDeviceModel() != null ? term.getDeviceModel().getDeviceName() : null)
                .deviceCategoryId(term.getDeviceCategory() != null ? term.getDeviceCategory().getDeviceCategoryId() : null)
                .deviceCategoryName(term.getDeviceCategory() != null ? term.getDeviceCategory().getDeviceCategoryName() : null)
                .createdAt(term.getCreatedAt())
                .updatedAt(term.getUpdatedAt())
                .build();
    }
}

