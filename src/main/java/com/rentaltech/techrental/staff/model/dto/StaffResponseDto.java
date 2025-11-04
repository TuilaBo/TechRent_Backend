package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.StaffRole;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StaffResponseDto {
    private Long staffId;
    private Long accountId;
    private String username;
    private String email;
    private String phoneNumber;
    private StaffRole staffRole;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
