package com.rentaltech.techrental.authentication.model.dto;

import com.rentaltech.techrental.authentication.model.Role;
import lombok.*;

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
    private Long customerId;
    private Long staffId;
}


