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
public class ProcessComplaintRequestDto {
    private String staffNote; // Ghi chú từ staff (optional)
    private ComplaintFaultSource faultSource;
    /**
     * Danh sách conditionDefinitionId phản ánh hư hỏng/thiệt hại do khách gây ra (nếu faultSource=CUSTOMER)
     */
    private List<Long> conditionDefinitionIds;
    private String damageNote;
}

