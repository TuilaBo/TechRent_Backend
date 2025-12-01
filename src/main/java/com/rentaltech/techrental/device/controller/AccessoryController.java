package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.device.model.dto.AccessoryRequestDto;
import com.rentaltech.techrental.device.service.AccessoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accessories")
@Tag(name = "Quản lý phụ kiện", description = "Các API tạo, xem, cập nhật và xóa phụ kiện thiết bị")
public class AccessoryController {

    private final AccessoryService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo phụ kiện", description = "Thêm mới một phụ kiện vào kho thiết bị")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo phụ kiện thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> create(@Valid @RequestBody AccessoryRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Phụ kiện được tạo thành công",
                "Phụ kiện đã được thêm vào hệ thống",
                service.create(request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết phụ kiện", description = "Lấy thông tin phụ kiện theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin phụ kiện"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phụ kiện"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getById(@Parameter(description = "Mã phụ kiện") @PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Phụ kiện tìm thấy",
                "Phụ kiện với mã " + id + " đã được tìm thấy",
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "Danh sách phụ kiện", description = "Liệt kê tất cả phụ kiện hiện có")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách phụ kiện"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getAll() {
        return ResponseUtil.createSuccessResponse(
                "Danh sách tất cả phụ kiện",
                "Danh sách tất cả phụ kiện trong hệ thống",
                service.findAll(),
                HttpStatus.OK
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật phụ kiện", description = "Chỉnh sửa thông tin phụ kiện theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật phụ kiện thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phụ kiện"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> update(@Parameter(description = "Mã phụ kiện") @PathVariable Long id,
                                    @Valid @RequestBody AccessoryRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Phụ kiện được cập nhật thành công",
                "Phụ kiện với mã " + id + " đã cập nhật vào hệ thống",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa phụ kiện", description = "Xóa phụ kiện khỏi hệ thống theo mã")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xóa phụ kiện thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy phụ kiện"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> delete(@Parameter(description = "Mã phụ kiện") @PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Phụ kiện được xóa thành công",
                "Phụ kiện với mã " + id + " đã xóa khỏi hệ thống",
                HttpStatus.NO_CONTENT
        );
    }
}

