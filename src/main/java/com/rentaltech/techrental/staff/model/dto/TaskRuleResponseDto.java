package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.TaskRule;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TaskRuleResponseDto {
    Long taskRuleId;
    String name;
    String description;
    Integer maxTasksPerDay;
    Boolean active;
    LocalDateTime effectiveFrom;
    LocalDateTime effectiveTo;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static TaskRuleResponseDto from(TaskRule rule) {
        if (rule == null) {
            return null;
        }
        return TaskRuleResponseDto.builder()
                .taskRuleId(rule.getTaskRuleId())
                .name(rule.getName())
                .description(rule.getDescription())
                .maxTasksPerDay(rule.getMaxTasksPerDay())
                .active(rule.getActive())
                .effectiveFrom(rule.getEffectiveFrom())
                .effectiveTo(rule.getEffectiveTo())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}

