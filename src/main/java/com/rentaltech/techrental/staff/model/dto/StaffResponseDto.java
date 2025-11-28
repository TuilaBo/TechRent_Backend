package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.StaffRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public static StaffResponseDto from(Staff staff) {
        if (staff == null) {
            return null;
        }
        var account = staff.getAccount();
        return StaffResponseDto.builder()
                .staffId(staff.getStaffId())
                .accountId(account != null ? account.getAccountId() : null)
                .username(account != null ? account.getUsername() : null)
                .email(account != null ? account.getEmail() : null)
                .phoneNumber(account != null ? account.getPhoneNumber() : null)
                .staffRole(staff.getStaffRole())
                .isActive(staff.getIsActive())
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .build();
    }
}
