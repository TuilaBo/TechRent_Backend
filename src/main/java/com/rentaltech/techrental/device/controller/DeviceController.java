package com.rentaltech.techrental.device.controller;

import com.rentaltech.techrental.device.model.dto.DeviceRequestDto;
import com.rentaltech.techrental.device.model.dto.DeviceResponseDto;
import com.rentaltech.techrental.device.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
@Tag(name = "Thiết bị", description = "API quản lý thiết bị")
public class DeviceController {

    private final DeviceService service;

    @PostMapping
    @Operation(summary = "Tạo thiết bị", description = "Tạo mới một thiết bị")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<DeviceResponseDto> create(@Valid @RequestBody DeviceRequestDto request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thiết bị theo ID", description = "Truy vấn thiết bị theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<DeviceResponseDto> getById(@Parameter(description = "ID thiết bị") @PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping
    @Operation(summary = "Danh sách thiết bị", description = "Lấy tất cả thiết bị")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<List<DeviceResponseDto>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thiết bị", description = "Cập nhật thiết bị theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<DeviceResponseDto> update(@Parameter(description = "ID thiết bị") @PathVariable Long id,
                                                    @Valid @RequestBody DeviceRequestDto request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá thiết bị", description = "Xoá thiết bị theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xoá thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống")
    })
    public ResponseEntity<Void> delete(@Parameter(description = "ID thiết bị") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
