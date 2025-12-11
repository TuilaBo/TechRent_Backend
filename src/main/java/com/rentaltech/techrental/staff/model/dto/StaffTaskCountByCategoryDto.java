package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.TaskCategoryType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StaffTaskCountByCategoryDto {
    Long staffId;
    String staffName;
    TaskCategoryType taskCategory;
    String taskCategoryDisplayName;
    Long taskCount;
    Integer maxTasksPerDay; // Giới hạn task trong ngày từ rule

    public static StaffTaskCountByCategoryDto from(Object[] record, Integer maxTasksPerDay) {
        return StaffTaskCountByCategoryDto.builder()
                .staffId((Long) record[0])
                .staffName((String) record[1])
                .taskCategory((TaskCategoryType) record[2])
                .taskCategoryDisplayName(record[2] instanceof TaskCategoryType type ? type.getName() : null)
                .taskCount((Long) record[3])
                .maxTasksPerDay(maxTasksPerDay)
                .build();
    }
}

