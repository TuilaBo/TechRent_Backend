package com.rentaltech.techrental.webapi.operator.service.impl;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.dto.TaskCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskResponseDto;
import com.rentaltech.techrental.staff.service.taskservice.TaskService;
import com.rentaltech.techrental.webapi.operator.service.OperatorTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OperatorTaskServiceImpl implements OperatorTaskService {
    
    private final TaskService taskService;
    
    @Override
    public Task createTask(TaskCreateRequestDto request) {
        return taskService.createTask(request);
    }
    
    @Override
    public Task getTaskById(Long taskId) {
        return taskService.getTaskById(taskId);
    }
    
    @Override
    public List<Task> getTasksByOrder(Long orderId) {
        return taskService.getTasksByOrder(orderId);
    }
    
    @Override
    public List<Task> getTasks(Long assignedStaffId) {
        List<Task> tasks = taskService.getAllTasks();
        
        // Filter by staff if provided
        if (assignedStaffId != null) {
            return tasks.stream()
                    .filter(task -> task.getAssignedStaff() != null && 
                                   task.getAssignedStaff().getStaffId().equals(assignedStaffId))
                    .toList();
        }
        
        return tasks;
    }
    
    @Override
    public TaskResponseDto mapToResponseDto(Task task) {
        TaskResponseDto.TaskResponseDtoBuilder builder = TaskResponseDto.builder()
                .taskId(task.getTaskId())
                .taskCategoryId(task.getTaskCategory().getTaskCategoryId())
                .taskCategoryName(task.getTaskCategory().getName())
                .orderId(task.getOrderId())
                .type(task.getType())
                .description(task.getDescription())
                .plannedStart(task.getPlannedStart())
                .plannedEnd(task.getPlannedEnd())
                .status(task.getStatus())
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt());

        // Add staff info if assigned
        if (task.getAssignedStaff() != null) {
            builder.assignedStaffId(task.getAssignedStaff().getStaffId())
                   .assignedStaffName(task.getAssignedStaff().getAccount().getUsername())
                   .assignedStaffRole(task.getAssignedStaff().getStaffRole().toString());
        }

        return builder.build();
    }
}

