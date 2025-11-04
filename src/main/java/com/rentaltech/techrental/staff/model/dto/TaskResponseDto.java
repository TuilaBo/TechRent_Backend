package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.TaskStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskResponseDto {
    private Long taskId;
    private Long taskCategoryId;
    private String taskCategoryName;
    private Long orderId;
    private Long assignedStaffId;
    private String assignedStaffName;
    private String assignedStaffRole;
    private String type;
    private String description;
    private LocalDateTime plannedStart;
    private LocalDateTime plannedEnd;
    private TaskStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
