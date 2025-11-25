package com.rentaltech.techrental.staff.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverReportBaseUpdateRequestDto {

    @NotBlank
    private String customerInfo;

    @NotBlank
    private String technicianInfo;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime handoverDateTime;

    @NotBlank
    private String handoverLocation;

    private String customerSignature;

    @Builder.Default
    private List<@Valid HandoverReportItemRequestDto> items = new ArrayList<>();
}
