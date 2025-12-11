package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskCategoryType;
import com.rentaltech.techrental.staff.model.TaskStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class StaffAssignmentDto {
    Long taskId;
    Long orderId;
    TaskCategoryType taskCategory;
    String taskCategoryDisplayName;
    String description;
    LocalDateTime plannedStart;
    LocalDateTime plannedEnd;
    TaskStatus status;

    public static StaffAssignmentDto from(Task task) {
        if (task == null) {
            return null;
        }
        TaskCategoryType category = task.getTaskCategory();
        return StaffAssignmentDto.builder()
                .taskId(task.getTaskId())
                .orderId(task.getOrderId())
                .taskCategory(category)
                .taskCategoryDisplayName(category != null ? category.getName() : null)
                .description(task.getDescription())
                .plannedStart(task.getPlannedStart())
                .plannedEnd(task.getPlannedEnd())
                .status(task.getStatus())
                .build();
    }
}

