package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.dto.*;
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

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff/tasks")
@Tag(name = "Quản lý tác vụ nhân sự", description = "API tạo, phân công, cập nhật và thống kê tác vụ cho nhân viên")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Tạo tác vụ", description = "Tạo tác vụ mới cho nhân viên")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo tác vụ thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    public ResponseEntity<?> createTask(@RequestBody @Valid TaskCreateRequestDto request,
                                        Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        Task savedTask = taskService.createTask(request, username);
        return ResponseUtil.createSuccessResponse(
                "Tạo tác vụ thành công",
                "Tác vụ đã được tạo",
                TaskResponseDto.from(savedTask),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Danh sách tác vụ", description = "Lấy tác vụ với các bộ lọc tùy chọn")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách tác vụ thành công"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getTasks(@RequestParam(required = false) Long categoryId,
                                      @RequestParam(required = false) Long orderId,
                                      @RequestParam(required = false) Long assignedStaffId,
                                      @RequestParam(required = false) String status,
                                      Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        List<Task> tasks = taskService.getTasks(categoryId, orderId, assignedStaffId, status, username);

        List<TaskResponseDto> responseDtos = tasks.stream()
                .map(TaskResponseDto::from)
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
    @Operation(summary = "Chi tiết tác vụ", description = "Lấy tác vụ theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về chi tiết tác vụ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy tác vụ")
    })
    public ResponseEntity<?> getTaskById(@PathVariable Long taskId, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        Task task = taskService.getTaskById(taskId, username);

        return ResponseUtil.createSuccessResponse(
                "Lấy tác vụ thành công",
                "Chi tiết tác vụ",
                TaskResponseDto.from(task),
                HttpStatus.OK
        );
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Tác vụ theo đơn hàng", description = "Lấy các tác vụ gắn với một đơn hàng cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về các tác vụ gắn với đơn hàng"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hàng hoặc không có tác vụ"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getTasksByOrder(@PathVariable Long orderId, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        List<Task> tasks = taskService.getTasksByOrder(orderId, username);

        List<TaskResponseDto> responseDtos = tasks.stream()
                .map(TaskResponseDto::from)
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
    @Operation(summary = "Cập nhật tác vụ", description = "Cập nhật tác vụ theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật tác vụ thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy tác vụ")
    })
    public ResponseEntity<?> updateTask(@PathVariable Long taskId,
                                        @RequestBody @Valid TaskUpdateRequestDto request,
                                        Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        Task updatedTask = taskService.updateTask(taskId, request, username);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật tác vụ thành công",
                "Tác vụ đã được cập nhật",
                TaskResponseDto.from(updatedTask),
                HttpStatus.OK
        );
    }

    @PatchMapping("/{taskId}/assign")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Gán tác vụ", description = "Gán tác vụ cho nhân viên")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gán tác vụ thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu gán tác vụ không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy tác vụ hoặc nhân viên")
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
                TaskResponseDto.from(updatedTask),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Xóa tác vụ", description = "Xóa tác vụ theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xóa tác vụ thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy tác vụ")
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
    @Operation(summary = "Nhân viên xác nhận đi giao", description = "Kỹ thuật viên/CSKH xác nhận sẽ thực hiện giao thiết bị")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xác nhận giao thiết bị thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền xác nhận tác vụ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy tác vụ")
    })
    public ResponseEntity<?> confirmDelivery(@PathVariable Long taskId, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        Task updated = taskService.confirmDelivery(taskId, username);
        return ResponseUtil.createSuccessResponse(
                "Xác nhận giao hàng thành công",
                "Tác vụ đã được đánh dấu IN_PROGRESS bởi nhân viên",
                TaskResponseDto.from(updated),
                HttpStatus.OK
        );
    }

    @PatchMapping("/{taskId}/confirm-retrieval")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Nhân viên xác nhận thu hồi", description = "Kỹ thuật viên/CSKH xác nhận sẽ thu hồi thiết bị cho tác vụ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xác nhận thu hồi thiết bị thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền xác nhận tác vụ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy tác vụ")
    })
    public ResponseEntity<?> confirmRetrieval(@PathVariable Long taskId, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        Task updated = taskService.confirmRetrieval(taskId, username);
        return ResponseUtil.createSuccessResponse(
                "Xác nhận thu hồi thành công",
                "Tác vụ đã được đánh dấu IN_PROGRESS bởi nhân viên",
                TaskResponseDto.from(updated),
                HttpStatus.OK
        );
    }

    @GetMapping("/staff-assignments")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Lịch làm việc trong ngày", description = "Xem công việc theo ngày cho từng nhân viên (tùy chọn lọc)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về lịch làm việc trong ngày"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getStaffAssignments(@RequestParam(required = false) Long staffId,
                                                 @RequestParam(required = false) @io.swagger.v3.oas.annotations.media.Schema(example = "2025-12-01") String date,
                                                 Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<StaffAssignmentDto> assignments = taskService.getStaffAssignmentsForDate(staffId, targetDate, username);
        return ResponseUtil.createSuccessResponse(
                "Danh sách công việc trong ngày",
                "Chi tiết công việc trong ngày",
                assignments,
                HttpStatus.OK
        );
    }

    @GetMapping("/completion-stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Thống kê công việc hoàn thành theo category trong tháng", description = "Trả về số lượng công việc đã hoàn thành theo từng nhóm")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thống kê công việc hoàn thành"),
            @ApiResponse(responseCode = "500", description = "Không thể thống kê do lỗi hệ thống")
    })
    public ResponseEntity<?> getCompletionStats(@RequestParam int year,
                                                @RequestParam int month,
                                                @RequestParam(required = false) Long taskCategoryId) {
        List<TaskCompletionStatsDto> stats = taskService.getMonthlyCompletionStats(year, month, taskCategoryId);
        return ResponseUtil.createSuccessResponse(
                "Thống kê hoàn thành",
                "Theo category",
                stats,
                HttpStatus.OK
        );
    }

    @GetMapping("/active-rule")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Rule tác vụ hiện hành", description = "Lấy rule tác vụ đang áp dụng trong hệ thống")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về rule tác vụ hiện hành"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy rule đang áp dụng")
    })
    public ResponseEntity<?> getActiveRule() {
        TaskRuleResponseDto rule = taskService.getActiveTaskRule();
        return ResponseUtil.createSuccessResponse(
                "Rule hiện hành",
                "",
                rule,
                HttpStatus.OK
        );
    }

    @GetMapping("/category-stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Thống kê công việc của nhân viên theo category", description = "Lấy số lượng công việc của nhân viên theo từng category trong ngày. Nếu không truyền staffId, tự động lấy từ tài khoản đang đăng nhập. Có thể filter theo categoryId cụ thể.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thống kê công việc theo category"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nhân viên")
    })
    public ResponseEntity<?> getStaffTaskCountByCategory(@RequestParam(required = false) Long staffId,
                                                          @RequestParam(required = false) @io.swagger.v3.oas.annotations.media.Schema(example = "2025-12-01") String date,
                                                          @RequestParam(required = false) Long categoryId,
                                                          Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<StaffTaskCountByCategoryDto> stats = taskService.getStaffTaskCountByCategory(staffId, targetDate, categoryId, username);
        return ResponseUtil.createSuccessResponse(
                "Thống kê công việc theo category",
                "Số lượng công việc của nhân viên theo từng category trong ngày",
                stats,
                HttpStatus.OK
        );
    }

}
