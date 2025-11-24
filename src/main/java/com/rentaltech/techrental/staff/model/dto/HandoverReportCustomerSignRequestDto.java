package com.rentaltech.techrental.staff.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverReportCustomerSignRequestDto {
    @NotBlank(message = "Mã PIN không được để trống")
    private String pinCode;
    
    private String customerSignature; // Optional signature data
}


