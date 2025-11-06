package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.StaffRole;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StaffCreateRequestDto {
    private Long accountId;
    private StaffRole staffRole;
}
