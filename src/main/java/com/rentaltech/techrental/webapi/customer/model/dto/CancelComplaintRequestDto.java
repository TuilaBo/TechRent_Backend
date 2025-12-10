package com.rentaltech.techrental.webapi.customer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelComplaintRequestDto {
    private String staffNote; // Ghi chú từ staff (optional)
}

