package com.rentaltech.techrental.staff.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TaskCompletionStatsDto {
    String taskCategoryName;
    Long completedCount;

    public static TaskCompletionStatsDto fromRecord(Object[] record) {
        if (record == null || record.length < 2) {
            return TaskCompletionStatsDto.builder()
                    .taskCategoryName(null)
                    .completedCount(0L)
                    .build();
        }
        String categoryName = record[0] != null ? record[0].toString() : null;
        Long count = record[1] instanceof Number ? ((Number) record[1]).longValue() : 0L;
        return TaskCompletionStatsDto.builder()
                .taskCategoryName(categoryName)
                .completedCount(count)
                .build();
    }
}

