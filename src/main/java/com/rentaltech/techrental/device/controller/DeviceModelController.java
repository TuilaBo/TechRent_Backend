package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.PageableUtil;
import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.device.model.dto.DeviceModelRequestDto;
import com.rentaltech.techrental.device.service.DeviceModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/device-models")
@Tag(name = "Quản lý mẫu thiết bị", description = "Các API phục vụ quản lý model thiết bị, bao gồm tạo mới, chỉnh sửa, tìm kiếm")
public class DeviceModelController {

    private final DeviceModelService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo mẫu thiết bị", description = "Thêm mới một model thiết bị kèm thông tin chi tiết")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo mẫu thiết bị thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> create(@RequestPart("request") @Valid DeviceModelRequestDto request,
                                    @Parameter(description = "Ảnh thiết bị", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
                                    @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        return ResponseUtil.createSuccessResponse(
                "Mẫu thiết bị được tạo thành công",
                "Mẫu thiết bị đã được thêm vào hệ thống",
                service.create(request, imageFile),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết mẫu thiết bị", description = "Lấy thông tin mẫu thiết bị theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin mẫu thiết bị"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy mẫu thiết bị"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getById(@Parameter(description = "Mã mẫu thiết bị") @PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Mẫu thiết bị tìm thấy",
                "Mẫu thiết bị với mã " + id + " đã được tìm thấy",
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "Danh sách mẫu thiết bị", description = "Liệt kê tất cả mẫu thiết bị trong hệ thống")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách mẫu thiết bị"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getAll() {
        return ResponseUtil.createSuccessResponse(
                "Danh sách tất cả mẫu thiết bị",
                "Danh sách tất cả mẫu thiết bị trong hệ thống",
                service.findAll(),
                HttpStatus.OK
        );
    }

    @GetMapping("/by-category/{categoryId}")
    @Operation(summary = "Danh sách mẫu thiết bị theo danh mục", description = "Trả về toàn bộ mẫu thiết bị thuộc một danh mục cụ thể")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách mẫu thiết bị"),
            @ApiResponse(responseCode = "400", description = "Mã danh mục không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getByCategory(@Parameter(description = "Mã danh mục thiết bị") @PathVariable Long categoryId) {
        return ResponseUtil.createSuccessResponse(
                "Danh sách mẫu thiết bị theo danh mục",
                "Các mẫu thiết bị thuộc danh mục mã " + categoryId,
                service.findByDeviceCategory(categoryId),
                HttpStatus.OK
        );
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật mẫu thiết bị", description = "Chỉnh sửa thông tin mẫu thiết bị theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật mẫu thiết bị thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy mẫu thiết bị"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> update(@Parameter(description = "Mã mẫu thiết bị") @PathVariable Long id,
                                    @RequestPart("request") @Valid DeviceModelRequestDto request,
                                    @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        return ResponseUtil.createSuccessResponse(
                "Mẫu thiết bị được cập nhật thành công",
                "Mẫu thiết bị với mã " + id + " đã được cập nhật",
                service.update(id, request, imageFile),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa mẫu thiết bị", description = "Xóa model thiết bị theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xóa mẫu thiết bị thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy mẫu thiết bị"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> delete(@Parameter(description = "Mã mẫu thiết bị") @PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Mẫu thiết bị được xóa thành công",
                "Mẫu thiết bị với mã " + id + " đã bị xóa khỏi hệ thống",
                HttpStatus.NO_CONTENT
        );
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm/lọc mẫu thiết bị", description = "Tra cứu mẫu thiết bị có hỗ trợ phân trang, sắp xếp và bộ lọc")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách thỏa điều kiện"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> search(
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long amountAvailable,
            @RequestParam(required = false) Long deviceCategoryId,
            @RequestParam(required = false) BigDecimal pricePerDay,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort) {
        Pageable pageable = PageableUtil.buildPageRequest(page, size, sort);
        var pageResult = service.search(deviceName, brandId, amountAvailable, deviceCategoryId, pricePerDay, isActive, pageable);
        return ResponseUtil.createSuccessPaginationResponse(
                "Kết quả tìm kiếm mẫu thiết bị",
                "Áp dụng phân trang/sắp xếp/lọc theo tham số",
                pageResult,
                HttpStatus.OK
        );
    }
}


