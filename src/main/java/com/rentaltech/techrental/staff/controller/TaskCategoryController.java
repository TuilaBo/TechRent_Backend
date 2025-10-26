package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.staff.model.TaskCategory;
import com.rentaltech.techrental.staff.model.dto.TaskCategoryCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.TaskCategoryUpdateRequestDto;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import com.rentaltech.techrental.staff.service.taskcategoryservice.TaskCategoryService;
import com.rentaltech.techrental.common.dto.AuthErrorResponseDto;
import com.rentaltech.techrental.common.dto.SuccessResponseDto;
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
    
    @Autowired
    private TaskCategoryRepository taskCategoryRepository;

    // Lấy tất cả task categories (READ ONLY)
    @GetMapping
    @Operation(summary = "List task categories", description = "Retrieve all task categories")
    public ResponseEntity<List<TaskCategory>> getAllTaskCategories() {
        List<TaskCategory> categories = taskCategoryService.getAllTaskCategories();
        return ResponseEntity.ok(categories);
    }

    // Lấy task category theo ID (READ ONLY)
    @GetMapping("/{categoryId}")
    @Operation(summary = "Get task category", description = "Retrieve task category by ID")
    public ResponseEntity<TaskCategory> getTaskCategoryById(@PathVariable Long categoryId) {
        return taskCategoryService.getTaskCategoryById(categoryId)
                .map(category -> ResponseEntity.ok(category))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Tìm task categories theo tên (READ ONLY)
    @GetMapping("/search")
    @Operation(summary = "Search task categories", description = "Search task categories by name")
    public ResponseEntity<List<TaskCategory>> searchTaskCategories(@RequestParam String name) {
        List<TaskCategory> categories = taskCategoryService.searchTaskCategoriesByName(name);
        return ResponseEntity.ok(categories);
    }

    // Kiểm tra task category có tồn tại không (READ ONLY)
    @GetMapping("/exists")
    @Operation(summary = "Check task category exists", description = "Check whether a task category name already exists")
    public ResponseEntity<Boolean> checkTaskCategoryExists(@RequestParam String name) {
        boolean exists = taskCategoryService.checkTaskCategoryExists(name);
        return ResponseEntity.ok(exists);
    }

    // ========== ADMIN APIs ==========
    
    // Tạo task category (ADMIN ONLY)
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create task category", description = "Create a new task category")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "409", description = "Already exists")
    })
    public ResponseEntity<?> createTaskCategory(@RequestBody @Valid TaskCategoryCreateRequestDto request) {
        try {
            // Kiểm tra tên category đã tồn tại chưa
            if (taskCategoryRepository.existsByName(request.getName())) {
                return ResponseUtil.createErrorResponse(
                        "CATEGORY_EXISTS",
                        "Tên danh mục đã tồn tại",
                        "Vui lòng chọn tên danh mục khác",
                        HttpStatus.CONFLICT
                );
            }
            
            TaskCategory category = TaskCategory.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .build();
            
            TaskCategory savedCategory = taskCategoryRepository.save(category);
            
            return ResponseUtil.createSuccessResponse(
                    "Tạo danh mục công việc thành công!",
                    "Danh mục đã được tạo và lưu vào hệ thống",
                    savedCategory,
                    HttpStatus.CREATED
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "CREATE_CATEGORY_FAILED",
                    "Tạo danh mục công việc thất bại",
                    "Có lỗi xảy ra khi tạo danh mục: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    // Cập nhật task category (ADMIN ONLY)
    @PutMapping("/admin/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update task category", description = "Update a task category by ID")
    public ResponseEntity<?> updateTaskCategory(@PathVariable Long categoryId, 
                                              @RequestBody @Valid TaskCategoryUpdateRequestDto request) {
        try {
            Optional<TaskCategory> categoryOpt = taskCategoryRepository.findById(categoryId);
            if (categoryOpt.isEmpty()) {
                return ResponseUtil.createErrorResponse(
                        "CATEGORY_NOT_FOUND",
                        "Không tìm thấy danh mục",
                        "Danh mục với ID " + categoryId + " không tồn tại",
                        HttpStatus.NOT_FOUND
                );
            }
            
            TaskCategory category = categoryOpt.get();
            
            // Kiểm tra tên mới có trùng với category khác không
            if (!category.getName().equals(request.getName()) && 
                taskCategoryRepository.existsByName(request.getName())) {
                return ResponseUtil.createErrorResponse(
                        "CATEGORY_EXISTS",
                        "Tên danh mục đã tồn tại",
                        "Vui lòng chọn tên danh mục khác",
                        HttpStatus.CONFLICT
                );
            }
            
            category.setName(request.getName());
            category.setDescription(request.getDescription());
            
            TaskCategory updatedCategory = taskCategoryRepository.save(category);
            
            return ResponseUtil.createSuccessResponse(
                    "Cập nhật danh mục công việc thành công!",
                    "Thông tin danh mục đã được cập nhật",
                    updatedCategory,
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "UPDATE_CATEGORY_FAILED",
                    "Cập nhật danh mục công việc thất bại",
                    "Có lỗi xảy ra khi cập nhật danh mục: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    // Xóa task category (ADMIN ONLY)
    @DeleteMapping("/admin/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete task category", description = "Delete a task category by ID")
    public ResponseEntity<?> deleteTaskCategory(@PathVariable Long categoryId) {
        try {
            if (!taskCategoryRepository.existsById(categoryId)) {
                return ResponseUtil.createErrorResponse(
                        "CATEGORY_NOT_FOUND",
                        "Không tìm thấy danh mục",
                        "Danh mục với ID " + categoryId + " không tồn tại",
                        HttpStatus.NOT_FOUND
                );
            }
            
            taskCategoryRepository.deleteById(categoryId);
            
            return ResponseUtil.createSuccessResponse(
                    "Xóa danh mục công việc thành công!",
                    "Danh mục đã được xóa khỏi hệ thống",
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "DELETE_CATEGORY_FAILED",
                    "Xóa danh mục công việc thất bại",
                    "Có lỗi xảy ra khi xóa danh mục: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
