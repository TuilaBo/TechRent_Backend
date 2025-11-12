package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.StaffRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffTaskCompletionStatsDto {
    private Long staffId;
    private Long accountId;
    private String username;
    private String email;
    private String phoneNumber;
    private StaffRole staffRole;
    private Long completedTaskCount;
}

