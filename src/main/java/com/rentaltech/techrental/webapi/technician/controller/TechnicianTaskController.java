package com.rentaltech.techrental.webapi.technician.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskStatus;
import com.rentaltech.techrental.staff.model.dto.TaskResponseDto;
import com.rentaltech.techrental.webapi.technician.service.TechnicianTaskService;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.authentication.model.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/technician/tasks")
@PreAuthorize("hasRole('TECHNICIAN')")
public class TechnicianTaskController {

    @Autowired
    private TechnicianTaskService technicianTaskService;

    @Autowired
    private AccountService accountService;

    // Lấy tất cả task được assign cho technician hiện tại
    @GetMapping
    public ResponseEntity<?> getMyTasks(Authentication authentication) {
        try {
            // Lấy thông tin technician hiện tại
            String username = authentication.getName();
            Account account = accountService.getByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Account not found"));
            
            // Lấy tất cả task được assign cho technician này
            List<Task> tasks = technicianTaskService.getTasksForTechnician(account.getAccountId());
            List<TaskResponseDto> responseDtos = tasks.stream()
                    .map(technicianTaskService::mapToResponseDto)
                    .collect(Collectors.toList());

            return ResponseUtil.createSuccessResponse(
                    "Lấy danh sách công việc thành công!",
                    "Danh sách công việc được phân công cho technician: " + username,
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

    // Lấy task theo ID (chỉ task được assign cho technician hiện tại)
    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTaskById(@PathVariable Long taskId, Authentication authentication) {
        try {
            // Lấy thông tin technician hiện tại
            String username = authentication.getName();
            Account account = accountService.getByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            // Lấy task
            Task task = technicianTaskService.getTaskForTechnician(taskId, account.getAccountId());
            TaskResponseDto responseDto = technicianTaskService.mapToResponseDto(task);

            return ResponseUtil.createSuccessResponse(
                    "Lấy thông tin công việc thành công!",
                    "Chi tiết công việc ID: " + taskId,
                    responseDto,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "GET_TASK_FAILED",
                    "Lấy thông tin công việc thất bại",
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // Lấy task theo trạng thái
    @GetMapping(params = "status")
    public ResponseEntity<?> getTasksByStatus(@RequestParam TaskStatus status, Authentication authentication) {
        try {
            String username = authentication.getName();
            Account account = accountService.getByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            List<Task> tasks = technicianTaskService.getTasksByStatusForTechnician(status, account.getAccountId());
            List<TaskResponseDto> responseDtos = tasks.stream()
                    .map(technicianTaskService::mapToResponseDto)
                    .collect(Collectors.toList());

            return ResponseUtil.createSuccessResponse(
                    "Lấy danh sách công việc theo trạng thái thành công!",
                    "Danh sách công việc có trạng thái: " + status + " cho technician: " + username,
                    responseDtos,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "GET_TASKS_BY_STATUS_FAILED",
                    "Lấy danh sách công việc theo trạng thái thất bại",
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // Lấy task theo order ID (chỉ task được assign cho technician hiện tại)
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getTasksByOrder(@PathVariable Long orderId, Authentication authentication) {
        try {
            String username = authentication.getName();
            Account account = accountService.getByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            List<Task> tasks = technicianTaskService.getTasksByOrderForTechnician(orderId, account.getAccountId());
            List<TaskResponseDto> responseDtos = tasks.stream()
                    .map(technicianTaskService::mapToResponseDto)
                    .collect(Collectors.toList());

            return ResponseUtil.createSuccessResponse(
                    "Lấy danh sách công việc theo đơn hàng thành công!",
                    "Danh sách công việc của đơn hàng ID: " + orderId + " cho technician: " + username,
                    responseDtos,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "GET_TASKS_BY_ORDER_FAILED",
                    "Lấy danh sách công việc theo đơn hàng thất bại",
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // Cập nhật trạng thái task (chỉ task được assign cho technician hiện tại)
    @PutMapping("/{taskId}/status")
    public ResponseEntity<?> updateTaskStatus(@PathVariable Long taskId, 
                                            @RequestParam TaskStatus status, 
                                            Authentication authentication) {
        try {
            String username = authentication.getName();
            Account account = accountService.getByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            Task updatedTask = technicianTaskService.updateTaskStatusForTechnician(taskId, status, account.getAccountId());
            TaskResponseDto responseDto = technicianTaskService.mapToResponseDto(updatedTask);

            return ResponseUtil.createSuccessResponse(
                    "Cập nhật trạng thái công việc thành công!",
                    "Trạng thái công việc đã được cập nhật thành: " + status,
                    responseDto,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "UPDATE_TASK_STATUS_FAILED",
                    "Cập nhật trạng thái công việc thất bại",
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // Lấy task quá hạn (chỉ task được assign cho technician hiện tại)
    @GetMapping("/overdue")
    public ResponseEntity<?> getOverdueTasks(Authentication authentication) {
        try {
            String username = authentication.getName();
            Account account = accountService.getByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Account not found"));

            List<Task> overdueTasks = technicianTaskService.getOverdueTasksForTechnician(account.getAccountId());
            List<TaskResponseDto> responseDtos = overdueTasks.stream()
                    .map(technicianTaskService::mapToResponseDto)
                    .collect(Collectors.toList());

            return ResponseUtil.createSuccessResponse(
                    "Lấy danh sách công việc quá hạn thành công!",
                    "Danh sách công việc quá hạn cho technician: " + username,
                    responseDtos,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "GET_OVERDUE_TASKS_FAILED",
                    "Lấy danh sách công việc quá hạn thất bại",
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
