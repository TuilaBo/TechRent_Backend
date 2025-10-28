package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.device.model.dto.BrandRequestDto;
import com.rentaltech.techrental.device.service.BrandService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/brands")
@Tag(name = "Brands", description = "Device brand management APIs")
public class BrandController {

    private final BrandService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create brand", description = "Create a new device brand")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> create(@Valid @RequestBody BrandRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Thương hiệu được tạo thành công",
                "Thương hiệu đã được thêm vào hệ thống",
                service.create(request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get brand by ID", description = "Retrieve brand by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> getById(@Parameter(description = "Brand ID") @PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Tìm thấy thương hiệu",
                "Thương hiệu với id " + id + " đã được tìm thấy",
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "List brands", description = "Retrieve all brands")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> getAll() {
        return ResponseUtil.createSuccessResponse(
                "Danh sách thương hiệu",
                "Danh sách tất cả thương hiệu trong hệ thống",
                service.findAll(),
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update brand", description = "Update brand by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> update(@Parameter(description = "Brand ID") @PathVariable Long id,
                                    @Valid @RequestBody BrandRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Cập nhật thương hiệu thành công",
                "Thương hiệu với id " + id + " đã được cập nhật",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete brand", description = "Delete brand by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> delete(@Parameter(description = "Brand ID") @PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Xóa thương hiệu thành công",
                "Thương hiệu với id " + id + " đã được xóa",
                HttpStatus.NO_CONTENT
        );
    }

    @GetMapping("/search")
    @Operation(summary = "Search/sort/filter brands", description = "Search brands with pagination, sorting and filtering")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public ResponseEntity<?> search(
            @RequestParam(required = false) String brandName,
            @RequestParam(required = false) Boolean isActive,
            Pageable pageable) {
        var page = service.search(brandName, isActive, pageable);
        return ResponseUtil.createSuccessPaginationResponse(
                "Kết quả tìm kiếm thương hiệu",
                "Áp dụng phân trang/sắp xếp/lọc theo tham số",
                page,
                HttpStatus.OK
        );
    }
}

