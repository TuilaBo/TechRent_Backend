package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.PageableUtil;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/brands")
@Tag(name = "Quản lý thương hiệu thiết bị", description = "Các API quản trị thương hiệu thiết bị: thêm mới, chỉnh sửa, tra cứu")
public class BrandController {

    private final BrandService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo thương hiệu", description = "Thêm mới một thương hiệu thiết bị")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo thương hiệu thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
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
    @Operation(summary = "Chi tiết thương hiệu", description = "Lấy thông tin thương hiệu theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin thương hiệu"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thương hiệu"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getById(@Parameter(description = "Mã thương hiệu") @PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Tìm thấy thương hiệu",
                "Thương hiệu với mã " + id + " đã được tìm thấy",
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "Danh sách thương hiệu", description = "Liệt kê tất cả thương hiệu thiết bị")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách thương hiệu"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
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
    @Operation(summary = "Cập nhật thương hiệu", description = "Chỉnh sửa thông tin thương hiệu theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thương hiệu thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thương hiệu"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> update(@Parameter(description = "Mã thương hiệu") @PathVariable Long id,
                                    @Valid @RequestBody BrandRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Cập nhật thương hiệu thành công",
                "Thương hiệu với mã " + id + " đã được cập nhật",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa thương hiệu", description = "Xóa thương hiệu khỏi hệ thống theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xóa thương hiệu thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thương hiệu"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> delete(@Parameter(description = "Mã thương hiệu") @PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Xóa thương hiệu thành công",
                "Thương hiệu với mã " + id + " đã được xóa",
                HttpStatus.NO_CONTENT
        );
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm/lọc thương hiệu", description = "Tra cứu thương hiệu có hỗ trợ phân trang, sắp xếp và lọc")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách thương hiệu đã áp dụng điều kiện"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> search(
            @RequestParam(required = false) String brandName,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort) {
        Pageable pageable = PageableUtil.buildPageRequest(page, size, sort);
        var pageResult = service.search(brandName, isActive, pageable);
        return ResponseUtil.createSuccessPaginationResponse(
                "Kết quả tìm kiếm thương hiệu",
                "Áp dụng phân trang/sắp xếp/lọc theo tham số",
                pageResult,
                HttpStatus.OK
        );
    }
}

