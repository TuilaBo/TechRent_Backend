package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.StaffRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverReportStaffDto {
    private Long staffId;
    private String fullName;
    private String username;
    private String phoneNumber;
    private String email;
    private StaffRole role;

    public static HandoverReportStaffDto fromEntity(Staff staff) {
        if (staff == null) {
            return null;
        }
        Account account = staff.getAccount();
        return HandoverReportStaffDto.builder()
                .staffId(staff.getStaffId())
                .fullName(account != null ? account.getUsername() : null) // fallback to username
                .username(account != null ? account.getUsername() : null)
                .phoneNumber(account != null ? account.getPhoneNumber() : null)
                .email(account != null ? account.getEmail() : null)
                .role(staff.getStaffRole())
                .build();
    }
}

