package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.device.model.dto.DeviceModelRequestDto;
import com.rentaltech.techrental.device.service.DeviceModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/device-models")
@Tag(name = "Device Models", description = "Device model management APIs")
public class DeviceModelController {

    private final DeviceModelService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create device model", description = "Create a new device model")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> create(@Valid @RequestBody DeviceModelRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Mẫu thiết bị được tạo thành công",
                "Mẫu thiết bị đã được thêm vào hệ thống",
                service.create(request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get device model by ID", description = "Retrieve device model by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> getById(@Parameter(description = "Device model ID") @PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Mẫu thiết bị tìm thấy",
                "Mẫu thiết bị với id " + id + " đã được tìm thấy",
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "List device models", description = "Retrieve all device models")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> getAll() {
        return ResponseUtil.createSuccessResponse(
                "Danh sách tất cả mẫu thiết bị",
                "Danh sách tất cả mẫu thiết bị trong hệ thống",
                service.findAll(),
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update device model", description = "Update device model by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> update(@Parameter(description = "Device model ID") @PathVariable Long id,
                                    @Valid @RequestBody DeviceModelRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Mẫu thiết bị được cập nhật thành công",
                "Mẫu thiết bị với id " + id + " đã được cập nhật",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete device model", description = "Delete device model by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> delete(@Parameter(description = "Device model ID") @PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Mẫu thiết bị được xóa thành công",
                "Mẫu thiết bị với id " + id + " đã bị xóa khỏi hệ thống",
                HttpStatus.NO_CONTENT
        );
    }

    @GetMapping("/search")
    @Operation(summary = "Search/sort/filter device models", description = "Search device models with pagination, sorting and filtering")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> search(
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long deviceCategoryId,
            @RequestParam(required = false) Boolean isActive,
            Pageable pageable) {
        var page = service.search(deviceName, brandId, deviceCategoryId, isActive, pageable);
        return ResponseUtil.createSuccessPaginationResponse(
                "Kết quả tìm kiếm mẫu thiết bị",
                "Áp dụng phân trang/sắp xếp/lọc theo tham số",
                page,
                HttpStatus.OK
        );
    }
}


