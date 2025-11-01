package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.DisputeOpenBy;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeCreateRequestDto {
    @NotNull
    private Long settlementId;
    
    @NotNull
    private DisputeOpenBy openBy;
    
    private Long customerId;  // Required if openBy = CUSTOMER
    
    private Long staffId;     // Required if openBy = TECHNICIAN/OPERATOR/ADMIN
    
    @NotNull
    private String reason;
    
    private String detail;
}

