package com.rentaltech.techrental.staff.model.dto;

import lombok.*;

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
