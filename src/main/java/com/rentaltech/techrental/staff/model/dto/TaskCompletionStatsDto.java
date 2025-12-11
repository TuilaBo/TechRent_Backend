package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.TaskCategoryType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TaskCompletionStatsDto {
    TaskCategoryType taskCategory;
    String taskCategoryDisplayName;
    Long completedCount;

    public static TaskCompletionStatsDto fromRecord(Object[] record) {
        if (record == null || record.length < 2) {
            return TaskCompletionStatsDto.builder()
                    .taskCategory(null)
                    .taskCategoryDisplayName(null)
                    .completedCount(0L)
                    .build();
        }
        TaskCategoryType taskCategory = record[0] instanceof TaskCategoryType type ? type : null;
        Long count = record[1] instanceof Number ? ((Number) record[1]).longValue() : 0L;
        return TaskCompletionStatsDto.builder()
                .taskCategory(taskCategory)
                .taskCategoryDisplayName(taskCategory != null ? taskCategory.getName() : null)
                .completedCount(count)
                .build();
    }
}

