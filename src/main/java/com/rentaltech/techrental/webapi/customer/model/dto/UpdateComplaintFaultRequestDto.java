package com.rentaltech.techrental.webapi.customer.model.dto;

import com.rentaltech.techrental.webapi.customer.model.ComplaintFaultSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateComplaintFaultRequestDto {
    private ComplaintFaultSource faultSource;
    private List<Long> conditionDefinitionIds;
    private String damageNote;
    private String staffNote;
}

