package com.rentaltech.techrental.webapi.operator.controller;

import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.dto.TaskCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskResponseDto;
import com.rentaltech.techrental.staff.service.taskservice.TaskService;
import com.rentaltech.techrental.common.dto.SuccessResponseDto;
import com.rentaltech.techrental.common.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/operator/tasks")
@PreAuthorize("hasRole('OPERATOR')")
@Tag(name = "Operator Tasks", description = "Operator task APIs")
public class OperatorTaskController {

    @Autowired
    private TaskService taskService;

    // Operator tạo task và assign cho staff cụ thể
    @PostMapping
    @Operation(summary = "Create task for staff", description = "Operator creates and assigns a task to staff")
    public ResponseEntity<?> createTaskForStaff(@RequestBody @Valid TaskCreateRequestDto request) {
        Task createdTask = taskService.createTask(request);
        TaskResponseDto responseDto = mapToResponseDto(createdTask);

        return ResponseUtil.createSuccessResponse(
                "Tạo công việc thành công!",
                "Công việc đã được tạo và assign cho nhân viên",
                responseDto,
                HttpStatus.CREATED
        );
    }

    // Operator xem task theo ID
    @GetMapping("/{taskId}")
    @Operation(summary = "Get task by ID", description = "Get task details by ID")
    public ResponseEntity<?> getTaskById(@PathVariable Long taskId) {
        Task task = taskService.getTaskById(taskId);
        TaskResponseDto responseDto = mapToResponseDto(task);
        
        return ResponseUtil.createSuccessResponse(
                "Lấy thông tin công việc thành công!",
                "Thông tin chi tiết công việc",
                responseDto,
                HttpStatus.OK
        );
    }

    // Operator xem tasks theo order ID
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get tasks by order", description = "List tasks for an order ID")
    public ResponseEntity<?> getTasksByOrder(@PathVariable Long orderId) {
        try {
            var tasks = taskService.getTasksByOrder(orderId);
            var responseDtos = tasks.stream()
                    .map(this::mapToResponseDto)
                    .toList();

            return ResponseUtil.createSuccessResponse(
                    "Lấy danh sách công việc theo đơn hàng thành công!",
                    "Danh sách công việc của đơn hàng ID: " + orderId,
                    responseDtos,
                    HttpStatus.OK
            );

        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "GET_ORDER_TASKS_FAILED",
                    "Lấy danh sách công việc theo đơn hàng thất bại",
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // Operator xem tasks theo staff (sử dụng query parameter)
    @GetMapping
    @Operation(summary = "List tasks by staff", description = "List tasks, optionally filter by assigned staff")
    public ResponseEntity<?> getTasksByStaff(@RequestParam(required = false) Long assignedStaffId) {
        try {
            var tasks = taskService.getAllTasks();
            
            // Filter theo staff nếu có parameter
            if (assignedStaffId != null) {
                tasks = tasks.stream()
                        .filter(task -> task.getAssignedStaff() != null && 
                                       task.getAssignedStaff().getStaffId().equals(assignedStaffId))
                        .toList();
            }
            
            var responseDtos = tasks.stream()
                    .map(this::mapToResponseDto)
                    .toList();

            return ResponseUtil.createSuccessResponse(
                    "Lấy danh sách công việc thành công!",
                    assignedStaffId != null ? 
                        "Danh sách công việc được assign cho nhân viên ID: " + assignedStaffId :
                        "Danh sách tất cả công việc trong hệ thống",
                    responseDtos,
                    HttpStatus.OK
            );

        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "GET_TASKS_FAILED",
                    "Lấy danh sách công việc thất bại",
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
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
