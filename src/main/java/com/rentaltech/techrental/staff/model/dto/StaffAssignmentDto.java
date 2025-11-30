package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class StaffAssignmentDto {
    Long taskId;
    Long orderId;
    Long taskCategoryId;
    String taskCategoryName;
    String description;
    LocalDateTime plannedStart;
    LocalDateTime plannedEnd;
    TaskStatus status;

    public static StaffAssignmentDto from(Task task) {
        if (task == null) {
            return null;
        }
        return StaffAssignmentDto.builder()
                .taskId(task.getTaskId())
                .orderId(task.getOrderId())
                .taskCategoryId(task.getTaskCategory() != null ? task.getTaskCategory().getTaskCategoryId() : null)
                .taskCategoryName(task.getTaskCategory() != null ? task.getTaskCategory().getName() : null)
                .description(task.getDescription())
                .plannedStart(task.getPlannedStart())
                .plannedEnd(task.getPlannedEnd())
                .status(task.getStatus())
                .build();
    }
}

