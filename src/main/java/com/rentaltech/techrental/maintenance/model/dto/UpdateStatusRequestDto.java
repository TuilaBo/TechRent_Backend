package com.rentaltech.techrental.maintenance.model.dto;

import com.rentaltech.techrental.maintenance.model.MaintenanceScheduleStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequestDto {
    @NotNull(message = "Status không được để trống")
    private MaintenanceScheduleStatus status;

    private List<String> evidenceUrls;
}

