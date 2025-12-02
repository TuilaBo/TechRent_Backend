package com.rentaltech.techrental.policy.model.dto;

import com.rentaltech.techrental.policy.model.Policy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponseDto {
    private Long policyId;
    private String title;
    private String description;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private String deletedBy;
    private LocalDateTime deletedAt;

    public static PolicyResponseDto from(Policy policy) {
        if (policy == null) {
            return null;
        }
        return PolicyResponseDto.builder()
                .policyId(policy.getPolicyId())
                .title(policy.getTitle())
                .description(policy.getDescription())
                .fileUrl(policy.getFileUrl())
                .fileName(policy.getFileName())
                .fileType(policy.getFileType())
                .effectiveFrom(policy.getEffectiveFrom())
                .effectiveTo(policy.getEffectiveTo())
                .createdBy(policy.getCreatedBy())
                .createdAt(policy.getCreatedAt())
                .updatedBy(policy.getUpdatedBy())
                .updatedAt(policy.getUpdatedAt())
                .deletedBy(policy.getDeletedBy())
                .deletedAt(policy.getDeletedAt())
                .build();
    }
}

