package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.dto.TaskCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskResponseDto;
import com.rentaltech.techrental.staff.model.dto.TaskUpdateRequestDto;
import com.rentaltech.techrental.staff.service.taskservice.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff/tasks")
@Tag(name = "Tasks", description = "Staff task management APIs")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Create task", description = "Create a new staff task")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<?> createTask(@RequestBody @Valid TaskCreateRequestDto request,
                                        Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        Task savedTask = taskService.createTask(request, username);
        return ResponseUtil.createSuccessResponse(
                "Tạo tác vụ thành công",
                "Tác vụ đã được tạo",
                mapToResponseDto(savedTask),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "List tasks", description = "Get tasks with optional filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success")
    })
    public ResponseEntity<?> getTasks(@RequestParam(required = false) Long categoryId,
                                      @RequestParam(required = false) Long orderId,
                                      @RequestParam(required = false) Long assignedStaffId,
                                      @RequestParam(required = false) String status,
                                      Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        List<Task> tasks = taskService.getTasks(categoryId, orderId, assignedStaffId, status, username);

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

    @GetMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Get task by ID", description = "Retrieve a task by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<?> getTaskById(@PathVariable Long taskId, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        Task task = taskService.getTaskById(taskId, username);

        return ResponseUtil.createSuccessResponse(
                "Lấy tác vụ thành công",
                "Chi tiết tác vụ",
                mapToResponseDto(task),
                HttpStatus.OK
        );
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Get tasks by order", description = "Retrieve tasks by order ID")
    public ResponseEntity<?> getTasksByOrder(@PathVariable Long orderId, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        List<Task> tasks = taskService.getTasksByOrder(orderId, username);

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

    @PutMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Update task", description = "Update a task by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<?> updateTask(@PathVariable Long taskId,
                                        @RequestBody @Valid TaskUpdateRequestDto request,
                                        Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        Task updatedTask = taskService.updateTask(taskId, request, username);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật tác vụ thành công",
                "Tác vụ đã được cập nhật",
                mapToResponseDto(updatedTask),
                HttpStatus.OK
        );
    }

    @PatchMapping("/{taskId}/assign")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Assign task", description = "Assign task to a staff member")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<?> assignTask(@PathVariable Long taskId,
                                        @RequestParam List<Long> assignedStaffIds,
                                        Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        TaskUpdateRequestDto request = TaskUpdateRequestDto.builder()
                .assignedStaffIds(assignedStaffIds)
                .build();
        Task updatedTask = taskService.updateTask(taskId, request, username);
        return ResponseUtil.createSuccessResponse(
                "Gán tác vụ thành công",
                "Tác vụ đã được gán cho nhân viên",
                mapToResponseDto(updatedTask),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Delete task", description = "Delete a task by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<?> deleteTask(@PathVariable Long taskId, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        taskService.deleteTask(taskId, username);
        return ResponseUtil.createSuccessResponse(
                "Xóa tác vụ thành công",
                "Tác vụ đã được xóa",
                HttpStatus.NO_CONTENT
        );
    }

    @PatchMapping("/{taskId}/confirm-delivery")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Confirm going to deliver", description = "Technician/support confirms they will deliver for the task")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Confirmed"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<?> confirmDelivery(@PathVariable Long taskId, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        Task updated = taskService.confirmDelivery(taskId, username);
        return ResponseUtil.createSuccessResponse(
                "Confirmed delivery",
                "Task marked as IN_PROGRESS by assigned staff",
                mapToResponseDto(updated),
                HttpStatus.OK
        );
    }

    @PatchMapping("/{taskId}/confirm-retrieval")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Confirm going to retrieve", description = "Technician/support confirms they will retrieve goods for the task")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Confirmed"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<?> confirmRetrieval(@PathVariable Long taskId, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        Task updated = taskService.confirmRetrieval(taskId, username);
        return ResponseUtil.createSuccessResponse(
                "Confirmed retrieval",
                "Task marked as IN_PROGRESS by assigned staff",
                mapToResponseDto(updated),
                HttpStatus.OK
        );
    }

    private TaskResponseDto mapToResponseDto(Task task) {
        List<TaskResponseDto.AssignedStaffSummary> assignedStaff = task.getAssignedStaff() == null
                ? List.of()
                : task.getAssignedStaff().stream()
                .map(staff -> TaskResponseDto.AssignedStaffSummary.builder()
                        .staffId(staff.getStaffId())
                        .staffName(staff.getAccount() != null ? staff.getAccount().getUsername() : null)
                        .staffRole(staff.getStaffRole() != null ? staff.getStaffRole().name() : null)
                        .build())
                .collect(Collectors.toList());

        return TaskResponseDto.builder()
                .taskId(task.getTaskId())
                .taskCategoryId(task.getTaskCategory().getTaskCategoryId())
                .taskCategoryName(task.getTaskCategory().getName())
                .orderId(task.getOrderId())
                .assignedStaff(assignedStaff)
                .description(task.getDescription())
                .plannedStart(task.getPlannedStart())
                .plannedEnd(task.getPlannedEnd())
                .status(task.getStatus())
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }
}
