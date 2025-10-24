package com.rentaltech.techrental.webapi.technician.service;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskStatus;
import com.rentaltech.techrental.staff.model.dto.TaskResponseDto;
import com.rentaltech.techrental.staff.model.dto.TaskUpdateRequestDto;
import com.rentaltech.techrental.staff.service.taskservice.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TechnicianTaskServiceImpl implements TechnicianTaskService {

    @Autowired
    private TaskService taskService;

    @Override
    public List<Task> getTasksForTechnician(Long technicianAccountId) {
        return taskService.getAllTasks().stream()
                .filter(task -> task.getAssignedStaff() != null && 
                              task.getAssignedStaff().getAccount().getAccountId().equals(technicianAccountId))
                .collect(Collectors.toList());
    }

    @Override
    public Task getTaskForTechnician(Long taskId, Long technicianAccountId) {
        Task task = taskService.getTaskById(taskId);
        
        if (task.getAssignedStaff() == null || 
            !task.getAssignedStaff().getAccount().getAccountId().equals(technicianAccountId)) {
            throw new RuntimeException("Task not assigned to this technician");
        }
        
        return task;
    }

    @Override
    public List<Task> getTasksByStatusForTechnician(TaskStatus status, Long technicianAccountId) {
        return getTasksForTechnician(technicianAccountId).stream()
                .filter(task -> task.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> getTasksByOrderForTechnician(Long orderId, Long technicianAccountId) {
        return taskService.getTasksByOrder(orderId).stream()
                .filter(task -> task.getAssignedStaff() != null && 
                              task.getAssignedStaff().getAccount().getAccountId().equals(technicianAccountId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> getOverdueTasksForTechnician(Long technicianAccountId) {
        return taskService.getOverdueTasks().stream()
                .filter(task -> task.getAssignedStaff() != null && 
                              task.getAssignedStaff().getAccount().getAccountId().equals(technicianAccountId))
                .collect(Collectors.toList());
    }

    @Override
    public Task updateTaskStatusForTechnician(Long taskId, TaskStatus status, Long technicianAccountId) {
        // Kiểm tra task có được assign cho technician này không
        getTaskForTechnician(taskId, technicianAccountId);
        
        // Tạo update request
        TaskUpdateRequestDto updateRequest = TaskUpdateRequestDto.builder()
                .status(status)
                .build();
        
        // Cập nhật task
        Task updatedTask = taskService.updateTask(taskId, updateRequest);
        
        // Nếu hoàn thành, set thời gian hoàn thành
        if (status == TaskStatus.COMPLETED) {
            updatedTask.setCompletedAt(java.time.LocalDateTime.now());
            taskService.updateTask(taskId, TaskUpdateRequestDto.builder()
                    .status(status)
                    .build());
        }
        
        return updatedTask;
    }

    @Override
    public TaskResponseDto mapToResponseDto(Task task) {
        return TaskResponseDto.builder()
                .taskId(task.getTaskId())
                .taskCategoryId(task.getTaskCategory().getTaskCategoryId())
                .taskCategoryName(task.getTaskCategory().getName())
                .orderId(task.getOrderId())
                .assignedStaffId(task.getAssignedStaff() != null ? task.getAssignedStaff().getStaffId() : null)
                .assignedStaffName(task.getAssignedStaff() != null ? task.getAssignedStaff().getAccount().getUsername() : null)
                .assignedStaffRole(task.getAssignedStaff() != null ? task.getAssignedStaff().getStaffRole().toString() : null)
                .type(task.getType())
                .description(task.getDescription())
                .plannedStart(task.getPlannedStart())
                .plannedEnd(task.getPlannedEnd())
                .status(task.getStatus())
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }
}
