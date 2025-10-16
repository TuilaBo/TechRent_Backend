package com.rentaltech.techrental.authentication.model.dto;

import com.rentaltech.techrental.authentication.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountMeResponse {
    private Long accountId;
    private String username;
    private String email;
    private Role role;
    private String phoneNumber;
    private Boolean isActive;
}


