package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.dto.TaskCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskResponseDto;
import com.rentaltech.techrental.staff.model.dto.TaskUpdateRequestDto;
import com.rentaltech.techrental.staff.service.taskservice.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    // Tạo task mới (Job Ticket)
    @PostMapping
    public ResponseEntity<TaskResponseDto> createTask(@RequestBody @Valid TaskCreateRequestDto request) {
        Task savedTask = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponseDto(savedTask));
    }

    // Lấy tasks với filter options
    @GetMapping
    public ResponseEntity<List<TaskResponseDto>> getTasks(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long assignedStaffId,
            @RequestParam(required = false) String status) {
        
        List<Task> tasks = taskService.getAllTasks();
        
        // Apply filters
        if (categoryId != null) {
            tasks = tasks.stream()
                    .filter(task -> task.getTaskCategory().getTaskCategoryId().equals(categoryId))
                    .collect(Collectors.toList());
        }
        
        if (orderId != null) {
            tasks = tasks.stream()
                    .filter(task -> task.getOrderId().equals(orderId))
                    .collect(Collectors.toList());
        }
        
        if (assignedStaffId != null) {
            tasks = tasks.stream()
                    .filter(task -> task.getAssignedStaff() != null && 
                                   task.getAssignedStaff().getStaffId().equals(assignedStaffId))
                    .collect(Collectors.toList());
        }
        
        if (status != null) {
            tasks = tasks.stream()
                    .filter(task -> task.getStatus().toString().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }
        
        List<TaskResponseDto> responseDtos = tasks.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    // Lấy task theo ID
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponseDto> getTaskById(@PathVariable Long taskId) {
        Task task = taskService.getTaskById(taskId);
        return ResponseEntity.ok(mapToResponseDto(task));
    }

    // Lấy tasks theo order ID
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<TaskResponseDto>> getTasksByOrder(@PathVariable Long orderId) {
        List<Task> tasks = taskService.getTasksByOrder(orderId);
        List<TaskResponseDto> responseDtos = tasks.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }


    // Cập nhật task
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponseDto> updateTask(@PathVariable Long taskId, 
                                                     @RequestBody @Valid TaskUpdateRequestDto request) {
        try {
            Task updatedTask = taskService.updateTask(taskId, request);
            return ResponseEntity.ok(mapToResponseDto(updatedTask));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Xóa task
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        try {
            taskService.deleteTask(taskId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }


    private TaskResponseDto mapToResponseDto(Task task) {
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

        // Thêm thông tin staff nếu có
        if (task.getAssignedStaff() != null) {
            builder.assignedStaffId(task.getAssignedStaff().getStaffId())
                   .assignedStaffName(task.getAssignedStaff().getAccount().getUsername())
                   .assignedStaffRole(task.getAssignedStaff().getStaffRole().toString());
        }

        return builder.build();
    }
}
