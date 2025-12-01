package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.device.model.dto.DeviceCategoryRequestDto;
import com.rentaltech.techrental.device.service.DeviceCategoryService;
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
@RequestMapping("/api/device-categories")
@Tag(name = "Quản lý danh mục thiết bị", description = "Các API quản trị danh mục thiết bị dùng để nhóm thiết bị theo loại")
public class DeviceCategoryController {

    private final DeviceCategoryService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo danh mục thiết bị", description = "Thêm mới danh mục thiết bị trong hệ thống")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo danh mục thiết bị thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> create(@Valid @RequestBody DeviceCategoryRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Danh mục thiết bị được tạo thành công",
                "Danh mục thiết bị đã được thêm vào hệ thống",
                service.create(request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết danh mục thiết bị", description = "Lấy thông tin danh mục thiết bị theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin danh mục"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục thiết bị"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getById(@Parameter(description = "Mã danh mục thiết bị") @PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Danh mục thiết bị tìm thấy",
                "Danh mục thiết bị với mã " + id + " đã được tìm thấy",
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "Danh sách danh mục thiết bị", description = "Liệt kê toàn bộ danh mục thiết bị hiện có")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách danh mục"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getAll() {
        return ResponseUtil.createSuccessResponse(
                "Danh sách tất cả danh mục thiết bị",
                "Danh sách tất cả danh mục thiết bị trong hệ thống",
                service.findAll(),
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật danh mục thiết bị", description = "Chỉnh sửa thông tin danh mục thiết bị theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật danh mục thiết bị thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục thiết bị"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> update(@Parameter(description = "Mã danh mục thiết bị") @PathVariable Long id,
                                    @Valid @RequestBody DeviceCategoryRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Danh mục thiết bị được cập nhật thành công",
                "Danh mục thiết bị với mã " + id + " đã được cập nhật",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa danh mục thiết bị", description = "Xóa danh mục thiết bị theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xóa danh mục thiết bị thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> delete(@Parameter(description = "Mã danh mục thiết bị") @PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Danh mục thiết bị được xóa thành công",
                "Danh mục thiết bị với mã " + id + " đã bị xóa khỏi hệ thống",
                HttpStatus.NO_CONTENT
        );
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm/lọc danh mục thiết bị", description = "Tra cứu danh mục với hỗ trợ phân trang, sắp xếp và lọc")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách thỏa điều kiện"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> search(
            @RequestParam(required = false) String deviceCategoryName,
            @RequestParam(required = false) Boolean isActive,
            Pageable pageable) {
        var page = service.search(deviceCategoryName, isActive, pageable);
        return ResponseUtil.createSuccessPaginationResponse(
                "Kết quả tìm kiếm danh mục thiết bị",
                "Áp dụng phân trang/sắp xếp/lọc theo tham số",
                page,
                HttpStatus.OK
        );
    }
}
