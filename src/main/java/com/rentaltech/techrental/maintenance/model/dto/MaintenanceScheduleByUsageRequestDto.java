package com.rentaltech.techrental.maintenance.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class MaintenanceScheduleByUsageRequestDto {
    @NotNull(message = "Số lần sử dụng không được để trống")
    @Positive(message = "Số lần sử dụng phải lớn hơn 0")
    private Integer usageCount;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Positive(message = "Số ngày bảo trì phải lớn hơn 0")
    private Integer durationDays;

    private String status;
}


