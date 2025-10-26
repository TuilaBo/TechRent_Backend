package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.device.model.dto.AccessoryCategoryRequestDto;
import com.rentaltech.techrental.device.service.AccessoryCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accessory-categories")
@Tag(name = "Accessory Categories", description = "Accessory category management APIs")
public class AccessoryCategoryController {

    private final AccessoryCategoryService service;

    @PostMapping
    @Operation(summary = "Create accessory category", description = "Create a new accessory category")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> create(@Valid @RequestBody AccessoryCategoryRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Danh mục phụ kiện được tạo thành công",
                "Danh mục phụ kiện đã được thêm vào hệ thống",
                service.create(request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get accessory category by ID", description = "Retrieve accessory category by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> getById(@Parameter(description = "Accessory category ID") @PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Danh mục phụ kiện tìm thấy",
                "Danh mục phụ kiện với id " + id + " đã được tìm thấy",
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "List accessory categories", description = "Retrieve all accessory categories")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> getAll() {
        return ResponseUtil.createSuccessResponse(
                "Danh sách tất cả danh mục phụ kiện",
                "Danh sách tất cả danh mục phụ kiện trong hệ thống",
                service.findAll(),
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update accessory category", description = "Update accessory category by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> update(@Parameter(description = "Accessory category ID") @PathVariable Long id,
                                    @Valid @RequestBody AccessoryCategoryRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Danh mục phụ kiện được cập nhật thành công",
                "Danh mục phụ kiện với id " + id + " đã cập nhật vào hệ thống",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete accessory category", description = "Delete accessory category by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> delete(@Parameter(description = "Accessory category ID") @PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Danh mục phụ kiện được xóa thành công",
                "Danh mục phụ kiện với id " + id + " đã xóa khỏi hệ thống",
                HttpStatus.NO_CONTENT
        );
    }
}

