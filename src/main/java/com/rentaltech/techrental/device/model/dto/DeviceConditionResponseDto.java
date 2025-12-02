package com.rentaltech.techrental.device.model.dto;

import com.rentaltech.techrental.device.model.DeviceCondition;
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
public class DeviceConditionResponseDto {
    private Long deviceConditionId;
    private Long deviceId;
    private Long conditionDefinitionId;
    private String conditionDefinitionName;
    private String severity;
    private String note;
    private List<String> images;
    private LocalDateTime capturedAt;
    private Long capturedByStaffId;

    public static DeviceConditionResponseDto from(DeviceCondition entity) {
        if (entity == null) {
            return null;
        }
        var definition = entity.getConditionDefinition();
        return DeviceConditionResponseDto.builder()
                .deviceConditionId(entity.getDeviceConditionId())
                .deviceId(entity.getDevice() != null ? entity.getDevice().getDeviceId() : null)
                .conditionDefinitionId(definition != null ? definition.getConditionDefinitionId() : null)
                .conditionDefinitionName(definition != null ? definition.getName() : null)
                .severity(entity.getSeverity())
                .note(entity.getNote())
                .images(entity.getImages() == null ? new ArrayList<>() : new ArrayList<>(entity.getImages()))
                .capturedAt(entity.getCapturedAt())
                .capturedByStaffId(entity.getCapturedBy() != null ? entity.getCapturedBy().getStaffId() : null)
                .build();
    }
}
