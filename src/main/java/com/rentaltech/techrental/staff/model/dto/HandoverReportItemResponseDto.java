package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.HandoverReportItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverReportItemResponseDto {
    private Long handoverReportItemId;
    private Long allocationId;
    private List<String> evidenceUrls;

    public static HandoverReportItemResponseDto fromEntity(HandoverReportItem item) {
        if (item == null) {
            return null;
        }
        return HandoverReportItemResponseDto.builder()
                .handoverReportItemId(item.getHandoverReportItemId())
                .allocationId(item.getAllocation() != null ? item.getAllocation().getAllocationId() : null)
                .evidenceUrls(item.getEvidenceUrls() == null ? List.of() : List.copyOf(item.getEvidenceUrls()))
                .build();
    }
}
