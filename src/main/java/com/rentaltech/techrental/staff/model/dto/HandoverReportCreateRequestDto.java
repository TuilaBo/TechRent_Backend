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

    @NotBlank
    private String pinCode;

    /**
     * Optional custom items. If empty, the system will populate from OrderDetail.
     */
    private List<HandoverReportItemDto> items;
}

