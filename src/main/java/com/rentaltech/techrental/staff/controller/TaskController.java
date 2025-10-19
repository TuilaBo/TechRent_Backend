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

    // Lấy tất cả tasks
    @GetMapping
    public ResponseEntity<List<TaskResponseDto>> getAllTasks() {
        List<Task> tasks = taskService.getAllTasks();
        List<TaskResponseDto> responseDtos = tasks.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    // Lấy task theo ID
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponseDto> getTaskById(@PathVariable Long taskId) {
        return taskService.getTaskById(taskId)
                .map(task -> ResponseEntity.ok(mapToResponseDto(task)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Lấy tasks theo category
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<TaskResponseDto>> getTasksByCategory(@PathVariable Long categoryId) {
        List<Task> tasks = taskService.getTasksByCategory(categoryId);
        List<TaskResponseDto> responseDtos = tasks.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    // Lấy tasks theo order
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

    // Lấy tasks overdue
    @GetMapping("/overdue")
    public ResponseEntity<List<TaskResponseDto>> getOverdueTasks() {
        List<Task> overdueTasks = taskService.getOverdueTasks();
        List<TaskResponseDto> responseDtos = overdueTasks.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    private TaskResponseDto mapToResponseDto(Task task) {
        return TaskResponseDto.builder()
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
                .completedAt(task.getCompletedAt())
                .build();
    }
}
