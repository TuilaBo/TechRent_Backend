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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/device-models")
@Tag(name = "Mẫu thiết bị", description = "API quản lý mẫu thiết bị")
public class DeviceModelController {

    private final DeviceModelService service;

    @PostMapping
    @Operation(summary = "Tạo mẫu thiết bị", description = "Tạo mới một mẫu thiết bị")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
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
    @Operation(summary = "Lấy mẫu thiết bị theo ID", description = "Truy vấn mẫu thiết bị theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> getById(@Parameter(description = "ID mẫu thiết bị") @PathVariable Long id) {
        return ResponseUtil.createSuccessResponse(
                "Mẫu thiết bị tìm thấy",
                "Mẫu thiết bị với id " + id + " đã được tìm thấy",
                service.findById(id),
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "Danh sách mẫu thiết bị", description = "Lấy tất cả mẫu thiết bị")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công"),
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

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật mẫu thiết bị", description = "Cập nhật mẫu thiết bị theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> update(@Parameter(description = "ID mẫu thiết bị") @PathVariable Long id,
                                    @Valid @RequestBody DeviceModelRequestDto request) {
        return ResponseUtil.createSuccessResponse(
                "Mẫu thiết bị được cập nhật thành công",
                "Mẫu thiết bị với id " + id + " đã cập nhật vào hệ thống",
                service.update(id, request),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá mẫu thiết bị", description = "Xoá mẫu thiết bị theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xoá thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<?> delete(@Parameter(description = "ID mẫu thiết bị") @PathVariable Long id) {
        service.delete(id);
        return ResponseUtil.createSuccessResponse(
                "Mẫu thiết bị được xóa thành công",
                "Mẫu thiết bị với id " + id + " đã xóa khỏi hệ thống",
                HttpStatus.NO_CONTENT
        );
    }
}
