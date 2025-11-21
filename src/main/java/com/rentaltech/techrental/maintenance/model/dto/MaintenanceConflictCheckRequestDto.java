package com.rentaltech.techrental.maintenance.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceConflictCheckRequestDto {
    @NotNull(message = "Danh sách device IDs không được để trống")
    private List<Long> deviceIds;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}

