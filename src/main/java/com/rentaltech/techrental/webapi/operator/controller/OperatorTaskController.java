package com.rentaltech.techrental.webapi.operator.controller;

import com.rentaltech.techrental.staff.model.dto.TaskCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskResponseDto;
import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.webapi.operator.service.OperatorTaskService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operator/tasks")
@PreAuthorize("hasRole('OPERATOR')")
@Tag(name = "Operator Tasks", description = "Operator task APIs")
@RequiredArgsConstructor
public class OperatorTaskController {

    private final OperatorTaskService operatorTaskService;

    @PostMapping
    @Operation(summary = "Create task for staff", description = "Operator creates and assigns a task to staff")
    public ResponseEntity<?> createTaskForStaff(@RequestBody @Valid TaskCreateRequestDto request) {
        var task = operatorTaskService.createTask(request);
        TaskResponseDto responseDto = operatorTaskService.mapToResponseDto(task);

        return ResponseUtil.createSuccessResponse(
                "Tạo công việc thành công!",
                "Công việc đã được tạo và assign cho nhân viên",
                responseDto,
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get task by ID", description = "Get task details by ID")
    public ResponseEntity<?> getTaskById(@PathVariable Long taskId) {
        var task = operatorTaskService.getTaskById(taskId);
        TaskResponseDto responseDto = operatorTaskService.mapToResponseDto(task);
        
        return ResponseUtil.createSuccessResponse(
                "Lấy thông tin công việc thành công!",
                "Thông tin chi tiết công việc",
                responseDto,
                HttpStatus.OK
        );
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get tasks by order", description = "List tasks for an order ID")
    public ResponseEntity<?> getTasksByOrder(@PathVariable Long orderId) {
        try {
            List<TaskResponseDto> responseDtos = operatorTaskService.getTasksByOrder(orderId).stream()
                    .map(operatorTaskService::mapToResponseDto)
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

    @GetMapping
    @Operation(summary = "List tasks by staff", description = "List tasks, optionally filter by assigned staff")
    public ResponseEntity<?> getTasksByStaff(@RequestParam(required = false) Long assignedStaffId) {
        try {
            List<TaskResponseDto> responseDtos = operatorTaskService.getTasks(assignedStaffId).stream()
                    .map(operatorTaskService::mapToResponseDto)
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
}
