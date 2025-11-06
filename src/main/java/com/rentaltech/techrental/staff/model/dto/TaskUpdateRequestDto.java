package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.TaskStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskUpdateRequestDto {
    private Long taskCategoryId;
    private Long assignedStaffId;
    private String type;
    private String description;
    private LocalDateTime plannedStart;
    private LocalDateTime plannedEnd;
    private TaskStatus status;
}
