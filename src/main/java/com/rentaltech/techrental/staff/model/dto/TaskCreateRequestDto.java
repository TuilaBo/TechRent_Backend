package com.rentaltech.techrental.staff.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskCreateRequestDto {
    private Long taskCategoryId;
    private Long orderId;
    private Long assignedStaffId; // ID của staff được assign task
    private String type;
    private String description;
    private LocalDateTime plannedStart;
    private LocalDateTime plannedEnd;
}
