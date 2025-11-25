package com.rentaltech.techrental.staff.model.dto;

import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
    private List<AssignedStaffSummary> assignedStaff;
    private String description;
    private LocalDateTime plannedStart;
    private LocalDateTime plannedEnd;
    private TaskStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static TaskResponseDto from(Task task) {
        if (task == null) {
            return null;
        }
        var category = task.getTaskCategory();
        List<AssignedStaffSummary> assignedStaffDtos = task.getAssignedStaff() == null
                ? List.of()
                : task.getAssignedStaff().stream()
                .filter(Objects::nonNull)
                .map(AssignedStaffSummary::fromStaff)
                .toList();
        return TaskResponseDto.builder()
                .taskId(task.getTaskId())
                .taskCategoryId(category != null ? category.getTaskCategoryId() : null)
                .taskCategoryName(category != null ? category.getName() : null)
                .orderId(task.getOrderId())
                .assignedStaff(assignedStaffDtos)
                .description(task.getDescription())
                .plannedStart(task.getPlannedStart())
                .plannedEnd(task.getPlannedEnd())
                .status(task.getStatus())
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AssignedStaffSummary {
        private Long staffId;
        private String staffName;
        private String staffRole;

        public static AssignedStaffSummary fromStaff(Staff staff) {
            if (staff == null) {
                return AssignedStaffSummary.builder().build();
            }
            var account = staff.getAccount();
            return AssignedStaffSummary.builder()
                    .staffId(staff.getStaffId())
                    .staffName(account != null ? account.getUsername() : null)
                    .staffRole(staff.getStaffRole() != null ? staff.getStaffRole().name() : null)
                    .build();
        }
    }
}
