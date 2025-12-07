package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.staff.model.dto.TaskRuleRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskRuleResponseDto;
import com.rentaltech.techrental.staff.service.taskruleservice.TaskRuleService;
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

@RestController
@RequestMapping("/api/admin/task-rules")
@RequiredArgsConstructor
@Tag(name = "Quy tắc tác vụ", description = "API quản lý rule áp dụng cho việc phân công công việc")
public class TaskRuleController {

    private final TaskRuleService taskRuleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo rule mới")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo rule thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    public ResponseEntity<?> create(@RequestBody @Valid TaskRuleRequestDto request, Authentication authentication) {
        TaskRuleResponseDto response = taskRuleService.create(request, authentication != null ? authentication.getName() : null);
        return ResponseUtil.createSuccessResponse("Tạo rule thành công", "Rule đã được tạo", response, HttpStatus.CREATED);
    }

    @PutMapping("/{taskRuleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật rule")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật rule thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy rule")
    })
    public ResponseEntity<?> update(@PathVariable Long taskRuleId,
                                    @RequestBody @Valid TaskRuleRequestDto request,
                                    Authentication authentication) {
        TaskRuleResponseDto response = taskRuleService.update(taskRuleId, request, authentication != null ? authentication.getName() : null);
        return ResponseUtil.createSuccessResponse("Cập nhật rule thành công", "Rule đã được cập nhật", response, HttpStatus.OK);
    }

    @DeleteMapping("/{taskRuleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xoá rule")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa rule thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy rule")
    })
    public ResponseEntity<?> delete(@PathVariable Long taskRuleId) {
        taskRuleService.delete(taskRuleId);
        return ResponseUtil.createSuccessResponse("Đã xoá rule", "Rule đã được xoá khỏi hệ thống", HttpStatus.OK);
    }

    @GetMapping("/{taskRuleId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Xem chi tiết rule")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về chi tiết rule"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy rule")
    })
    public ResponseEntity<?> get(@PathVariable Long taskRuleId) {
        TaskRuleResponseDto response = taskRuleService.get(taskRuleId);
        return ResponseUtil.createSuccessResponse("Chi tiết rule", "", response, HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    @Operation(summary = "Danh sách rule")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách rule"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> list(@RequestParam(required = false) Boolean active) {
        List<TaskRuleResponseDto> responses = taskRuleService.list(active);
        return ResponseUtil.createSuccessResponse("Danh sách rule", "", responses, HttpStatus.OK);
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Danh sách rule đang áp dụng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách rule đang được áp dụng"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<?> getActiveRules() {
        List<TaskRuleResponseDto> responses = taskRuleService.getActiveRules();
        return ResponseUtil.createSuccessResponse("Danh sách rule hiện hành", "", responses, HttpStatus.OK);
    }
}

