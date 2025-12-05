package com.rentaltech.techrental.staff.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StaffTaskCountByCategoryDto {
    Long staffId;
    String staffName;
    Long taskCategoryId;
    String taskCategoryName;
    Long taskCount;
    Integer maxTasksPerDay; // Giới hạn task trong ngày từ rule
    
    public static StaffTaskCountByCategoryDto from(Object[] record, Integer maxTasksPerDay) {
        return StaffTaskCountByCategoryDto.builder()
                .staffId((Long) record[0])
                .staffName((String) record[1])
                .taskCategoryId((Long) record[2])
                .taskCategoryName((String) record[3])
                .taskCount((Long) record[4])
                .maxTasksPerDay(maxTasksPerDay)
                .build();
    }
}

