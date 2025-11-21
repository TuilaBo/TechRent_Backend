package com.rentaltech.techrental.staff.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverReportCreateRequestDto {

    @NotNull
    private Long taskId;

    @NotBlank
    private String customerInfo;

    @NotBlank
    private String technicianInfo;

    @NotNull
    private LocalDateTime handoverDateTime;

    @NotBlank
    private String handoverLocation;

    private String customerSignature;

    /**
     * Optional custom items. If empty, the system will populate from OrderDetail.
     */
    private List<HandoverReportItemDto> items;

    /**
     * Device quality information by serial number.
     * Example: serial "A2j3j" has "MINOR_DAMAGE" with description "Bị hư màn hình nhẹ"
     */
    private List<DeviceQualityInfoDto> deviceQualityInfos;
}

