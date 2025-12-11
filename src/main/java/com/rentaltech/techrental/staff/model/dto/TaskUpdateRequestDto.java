package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.TaskCategoryType;
import com.rentaltech.techrental.staff.model.TaskStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskUpdateRequestDto {
    private TaskCategoryType taskCategory;
    private List<Long> assignedStaffIds;
    private String type;
    private String description;
    private LocalDateTime plannedStart;
    private LocalDateTime plannedEnd;
    private TaskStatus status;
}
