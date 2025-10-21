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
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accessories")
@Tag(name = "Phụ kiện", description = "API quản lý phụ kiện")
public class AccessoryController {

    private final AccessoryService service;

    @PostMapping
    @Operation(summary = "Tạo phụ kiện", description = "Tạo mới một phụ kiện")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo thành công"),
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
    @Operation(summary = "Lấy phụ kiện theo ID", description = "Truy vấn phụ kiện theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getById(@Parameter(description = "ID phụ kiện") @PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Phụ kiện tìm thấy",
                "Phụ kiện với id " + id + " đã được tìm thấy",
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "Danh sách phụ kiện", description = "Lấy tất cả phụ kiện")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công"),
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
    @Operation(summary = "Cập nhật phụ kiện", description = "Cập nhật phụ kiện theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> update(@Parameter(description = "ID phụ kiện") @PathVariable Long id,
                                    @Valid @RequestBody AccessoryRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Phụ kiện được cập nhật thành công",
                "Phụ kiện với id " + id + " đã cập nhật vào hệ thống",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá phụ kiện", description = "Xoá phụ kiện theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xoá thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> delete(@Parameter(description = "ID phụ kiện") @PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Phụ kiện được xóa thành công",
                "Phụ kiện với id " + id + " đã xóa khỏi hệ thống",
                HttpStatus.NO_CONTENT
        );
    }
}
