package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.DisputeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeUpdateRequestDto {
    private String reason;
    private String detail;
    private DisputeStatus status;
}

