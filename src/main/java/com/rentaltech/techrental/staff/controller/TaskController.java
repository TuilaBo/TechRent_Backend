package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.dto.TaskCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskResponseDto;
import com.rentaltech.techrental.staff.model.dto.TaskUpdateRequestDto;
import com.rentaltech.techrental.staff.service.taskservice.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import com.rentaltech.techrental.common.util.ResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/staff/tasks")
@Tag(name = "Tasks", description = "Staff task management APIs")
public class TaskController {

    @Autowired
    private TaskService taskService;

    // Tạo task mới (Job Ticket)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Create task", description = "Create a new staff task")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<?> createTask(@RequestBody @Valid TaskCreateRequestDto request) {
        Task savedTask = taskService.createTask(request);
        return ResponseUtil.createSuccessResponse(
                "Tạo tác vụ thành công",
                "Tác vụ đã được tạo",
                mapToResponseDto(savedTask),
                HttpStatus.CREATED
        );
    }

    // Lấy tasks với filter options
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "List tasks", description = "Get tasks with optional filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success")
    })
    public ResponseEntity<?> getTasks(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long assignedStaffId,
            @RequestParam(required = false) String status) {

        List<Task> tasks = taskService.getTasks(categoryId, orderId, assignedStaffId, status);
        List<TaskResponseDto> responseDtos = tasks.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách tác vụ thành công",
                "Danh sách tác vụ",
                responseDtos,
                HttpStatus.OK
        );
    }

    // Lấy task theo ID
    @GetMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Get task by ID", description = "Retrieve a task by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<?> getTaskById(@PathVariable Long taskId) {
        Task task = taskService.getTaskById(taskId);
        return ResponseUtil.createSuccessResponse(
                "Lấy tác vụ thành công",
                "Chi tiết tác vụ",
                mapToResponseDto(task),
                HttpStatus.OK
        );
    }

    // Lấy tasks theo order ID
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Get tasks by order", description = "Retrieve tasks by order ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success")
    })
    public ResponseEntity<?> getTasksByOrder(@PathVariable Long orderId) {
        List<Task> tasks = taskService.getTasksByOrder(orderId);
        List<TaskResponseDto> responseDtos = tasks.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách tác vụ theo đơn hàng thành công",
                "Danh sách tác vụ theo đơn hàng",
                responseDtos,
                HttpStatus.OK
        );
    }


    // Cập nhật task
    @PutMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Update task", description = "Update a task by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<?> updateTask(@PathVariable Long taskId,
                                                     @RequestBody @Valid TaskUpdateRequestDto request) {
        Task updatedTask = taskService.updateTask(taskId, request);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật tác vụ thành công",
                "Tác vụ đã được cập nhật",
                mapToResponseDto(updatedTask),
                HttpStatus.OK
        );
    }

    // Xóa task
    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Delete task", description = "Delete a task by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<?> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseUtil.createSuccessResponse(
                "Xóa tác vụ thành công",
                "Tác vụ đã được xóa",
                HttpStatus.NO_CONTENT
        );
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
