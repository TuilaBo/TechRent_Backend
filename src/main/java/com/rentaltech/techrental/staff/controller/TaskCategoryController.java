package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.staff.model.TaskCategory;
import com.rentaltech.techrental.staff.model.dto.TaskCategoryCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskCategoryUpdateRequestDto;
import com.rentaltech.techrental.staff.service.taskcategoryservice.TaskCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/task-categories")
@Tag(name = "Danh mục tác vụ", description = "API quản lý danh mục công việc cho nhân viên")
public class TaskCategoryController {

    @Autowired
    private TaskCategoryService taskCategoryService;

    // Get all task categories (READ ONLY)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Danh sách danh mục tác vụ", description = "Trả về tất cả danh mục tác vụ")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách danh mục"),
            @ApiResponse(responseCode = "500", description = "Không thể truy vấn do lỗi hệ thống")
    })
    public ResponseEntity<List<TaskCategory>> getAllTaskCategories() {
        List<TaskCategory> categories = taskCategoryService.getAllTaskCategories();
        return ResponseEntity.ok(categories);
    }

    // Get task category by ID (READ ONLY)
    @GetMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Chi tiết danh mục tác vụ", description = "Lấy thông tin danh mục theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh mục công việc"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục")
    })
    public ResponseEntity<TaskCategory> getTaskCategoryById(@PathVariable Long categoryId) {
        return taskCategoryService.getTaskCategoryById(categoryId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Search task categories by name (READ ONLY)
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Tìm kiếm danh mục tác vụ", description = "Tìm kiếm danh mục theo tên")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách danh mục phù hợp"),
            @ApiResponse(responseCode = "500", description = "Không thể tìm kiếm do lỗi hệ thống")
    })
    public ResponseEntity<List<TaskCategory>> searchTaskCategories(@RequestParam String name) {
        List<TaskCategory> categories = taskCategoryService.searchTaskCategoriesByName(name);
        return ResponseEntity.ok(categories);
    }

    // Check if task category exists by name (READ ONLY)
    @GetMapping("/exists")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Kiểm tra danh mục tồn tại", description = "Kiểm tra tên danh mục đã tồn tại hay chưa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về kết quả kiểm tra danh mục"),
            @ApiResponse(responseCode = "500", description = "Không thể kiểm tra do lỗi hệ thống")
    })
    public ResponseEntity<Boolean> checkTaskCategoryExists(@RequestParam String name) {
        boolean exists = taskCategoryService.checkTaskCategoryExists(name);
        return ResponseEntity.ok(exists);
    }

    // ========== ADMIN APIs ==========

    // Create task category (ADMIN ONLY)
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo danh mục tác vụ", description = "Tạo mới danh mục công việc")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo danh mục thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu tạo danh mục không hợp lệ"),
            @ApiResponse(responseCode = "409", description = "Danh mục đã tồn tại trong hệ thống")
    })
    public ResponseEntity<?> createTaskCategory(@RequestBody @Valid TaskCategoryCreateRequestDto request) {
        TaskCategory savedCategory = taskCategoryService.createTaskCategory(request);
        return ResponseUtil.createSuccessResponse(
                "Tạo danh mục công việc thành công!",
                "Danh mục đã được tạo và lưu vào hệ thống",
                savedCategory,
                HttpStatus.CREATED
        );
    }

    // Update task category (ADMIN ONLY)
    @PutMapping("/admin/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật danh mục tác vụ", description = "Chỉnh sửa danh mục công việc theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật danh mục thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục")
    })
    public ResponseEntity<?> updateTaskCategory(@PathVariable Long categoryId,
                                                @RequestBody @Valid TaskCategoryUpdateRequestDto request) {
        TaskCategory updatedCategory = taskCategoryService.updateTaskCategory(categoryId, request);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật danh mục công việc thành công!",
                "Thông tin danh mục đã được cập nhật",
                updatedCategory,
                HttpStatus.OK
        );
    }

    // Delete task category (ADMIN ONLY)
    @DeleteMapping("/admin/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa danh mục tác vụ", description = "Xóa danh mục công việc theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa danh mục thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục")
    })
    public ResponseEntity<?> deleteTaskCategory(@PathVariable Long categoryId) {
        taskCategoryService.deleteTaskCategory(categoryId);
        return ResponseUtil.createSuccessResponse(
                "Xóa danh mục công việc thành công!",
                "Danh mục đã được xóa khỏi hệ thống",
                HttpStatus.OK
        );
    }
}

