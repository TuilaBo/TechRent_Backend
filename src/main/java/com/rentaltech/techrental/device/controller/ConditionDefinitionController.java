package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.device.model.dto.ConditionDefinitionRequestDto;
import com.rentaltech.techrental.device.service.ConditionDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conditions/definitions")
@RequiredArgsConstructor
@Tag(name = "Condition Definitions", description = "Manage device condition definitions")
public class ConditionDefinitionController {

    private final ConditionDefinitionService conditionDefinitionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Tạo condition definition mới")
    public ResponseEntity<?> create(@Valid @RequestBody ConditionDefinitionRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Tạo định nghĩa tình trạng thành công",
                "Condition definition mới đã được thêm",
                conditionDefinitionService.create(request),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
    @Operation(summary = "Cập nhật condition definition")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @RequestBody ConditionDefinitionRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Cập nhật định nghĩa tình trạng thành công",
                "Condition definition đã được cập nhật",
                conditionDefinitionService.update(id, request),
                HttpStatus.OK
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết condition definition")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Thông tin định nghĩa tình trạng",
                "Condition definition chi tiết",
                conditionDefinitionService.getById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "Danh sách condition definition", description = "Lọc theo deviceCategoryId nếu cần")
    public ResponseEntity<?> getAll(@RequestParam(required = false) Long deviceCategoryId) {
        var data = deviceCategoryId == null
                ? conditionDefinitionService.getAll()
                : conditionDefinitionService.getByDeviceCategory(deviceCategoryId);
        return ResponseUtil.createSuccessResponse(
                "Danh sách định nghĩa tình trạng",
                "Tất cả condition definition hiện có",
                data,
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa condition definition")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        conditionDefinitionService.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Condition definition đã bị xóa",
                "Định nghĩa tình trạng không còn trong hệ thống",
                HttpStatus.NO_CONTENT
        );
    }
}
