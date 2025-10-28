package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.staff.model.TaskCategory;
import com.rentaltech.techrental.staff.model.dto.TaskCategoryCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskCategoryUpdateRequestDto;
import com.rentaltech.techrental.staff.service.taskcategoryservice.TaskCategoryService;
import com.rentaltech.techrental.common.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/staff/task-categories")
@Tag(name = "Task Categories", description = "Task category management APIs")
public class TaskCategoryController {

    @Autowired
    private TaskCategoryService taskCategoryService;

    // Get all task categories (READ ONLY)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "List task categories", description = "Retrieve all task categories")
    public ResponseEntity<List<TaskCategory>> getAllTaskCategories() {
        List<TaskCategory> categories = taskCategoryService.getAllTaskCategories();
        return ResponseEntity.ok(categories);
    }

    // Get task category by ID (READ ONLY)
    @GetMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Get task category", description = "Retrieve task category by ID")
    public ResponseEntity<TaskCategory> getTaskCategoryById(@PathVariable Long categoryId) {
        return taskCategoryService.getTaskCategoryById(categoryId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Search task categories by name (READ ONLY)
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Search task categories", description = "Search task categories by name")
    public ResponseEntity<List<TaskCategory>> searchTaskCategories(@RequestParam String name) {
        List<TaskCategory> categories = taskCategoryService.searchTaskCategoriesByName(name);
        return ResponseEntity.ok(categories);
    }

    // Check if task category exists by name (READ ONLY)
    @GetMapping("/exists")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Check task category exists", description = "Check whether a task category name already exists")
    public ResponseEntity<Boolean> checkTaskCategoryExists(@RequestParam String name) {
        boolean exists = taskCategoryService.checkTaskCategoryExists(name);
        return ResponseEntity.ok(exists);
    }

    // ========== ADMIN APIs ==========

    // Create task category (ADMIN ONLY)
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create task category", description = "Create a new task category")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "409", description = "Already exists")
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
    @Operation(summary = "Update task category", description = "Update a task category by ID")
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
    @Operation(summary = "Delete task category", description = "Delete a task category by ID")
    public ResponseEntity<?> deleteTaskCategory(@PathVariable Long categoryId) {
        taskCategoryService.deleteTaskCategory(categoryId);
        return ResponseUtil.createSuccessResponse(
                "Xóa danh mục công việc thành công!",
                "Danh mục đã được xóa khỏi hệ thống",
                HttpStatus.OK
        );
    }
}

