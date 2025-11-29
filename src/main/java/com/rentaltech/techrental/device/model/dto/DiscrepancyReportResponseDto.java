package com.rentaltech.techrental.device.model.dto;

import com.rentaltech.techrental.device.model.DiscrepancyCreatedFrom;
import com.rentaltech.techrental.device.model.DiscrepancyReport;
import com.rentaltech.techrental.device.model.DiscrepancyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscrepancyReportResponseDto {
    private Long discrepancyReportId;
    private DiscrepancyCreatedFrom createdFrom;
    private Long refId;
    private DiscrepancyType discrepancyType;
    private Long conditionDefinitionId;
    private String conditionName;
    private Long deviceId;
    private String serialNumber;
    private String deviceModelName;
    private BigDecimal penaltyAmount;
    private String staffNote;
    private LocalDateTime createdAt;

    public static DiscrepancyReportResponseDto from(DiscrepancyReport entity) {
        if (entity == null) {
            return null;
        }
        var definition = entity.getConditionDefinition();
        var allocation = entity.getAllocation();
        var device = allocation != null ? allocation.getDevice() : null;
        var deviceModel = device != null ? device.getDeviceModel() : null;
        return DiscrepancyReportResponseDto.builder()
                .discrepancyReportId(entity.getDiscrepancyReportId())
                .createdFrom(entity.getCreatedFrom())
                .refId(entity.getRefId())
                .discrepancyType(entity.getDiscrepancyType())
                .conditionDefinitionId(definition != null ? definition.getConditionDefinitionId() : null)
                .conditionName(definition != null ? definition.getName() : null)
                .deviceId(device != null ? device.getDeviceId() : null)
                .serialNumber(device != null ? device.getSerialNumber() : null)
                .deviceModelName(deviceModel != null ? deviceModel.getDeviceName() : null)
                .penaltyAmount(entity.getPenaltyAmount())
                .staffNote(entity.getStaffNote())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
