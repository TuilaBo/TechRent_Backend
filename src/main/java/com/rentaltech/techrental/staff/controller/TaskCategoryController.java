package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.staff.model.TaskCategoryType;
import com.rentaltech.techrental.staff.service.taskcategoryservice.TaskCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/staff/task-categories")
@Tag(name = "Danh mục tác vụ", description = "API tra cứu danh mục tác vụ mặc định")
@RequiredArgsConstructor
public class TaskCategoryController {

    private final TaskCategoryService taskCategoryService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Danh sách danh mục tác vụ", description = "Trả về toàn bộ danh mục tác vụ cố định")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công")
    })
    public ResponseEntity<List<TaskCategoryType.TaskCategoryDefinition>> getAllTaskCategories() {
        return ResponseEntity.ok(taskCategoryService.getAllTaskCategories());
    }

    @GetMapping("/{taskCategoryId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Tìm danh mục theo ID", description = "Tìm enum theo thứ tự định danh (bắt đầu từ 1)")
    public ResponseEntity<TaskCategoryType.TaskCategoryDefinition> getTaskCategoryByNumericId(@PathVariable int taskCategoryId) {
        return taskCategoryService.getTaskCategoryById(taskCategoryId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Tìm kiếm danh mục tác vụ", description = "Tìm theo tên hiển thị hoặc mô tả")
    public ResponseEntity<List<TaskCategoryType.TaskCategoryDefinition>> searchTaskCategories(@RequestParam String name) {
        return ResponseEntity.ok(taskCategoryService.searchTaskCategoriesByName(name));
    }

    @GetMapping("/exists")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Kiểm tra danh mục tồn tại", description = "Kiểm tra tên danh mục có khớp enum hay không")
    public ResponseEntity<Boolean> checkTaskCategoryExists(@RequestParam String name) {
        return ResponseEntity.ok(taskCategoryService.checkTaskCategoryExists(name));
    }
}
