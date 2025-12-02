package com.rentaltech.techrental.policy.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRequestDto {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String description;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate effectiveFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate effectiveTo;
}

