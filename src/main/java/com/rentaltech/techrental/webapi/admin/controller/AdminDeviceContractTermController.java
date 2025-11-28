package com.rentaltech.techrental.webapi.admin.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.contract.model.dto.DeviceContractTermRequestDto;
import com.rentaltech.techrental.contract.model.dto.DeviceContractTermResponseDto;
import com.rentaltech.techrental.contract.service.DeviceContractTermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/device-terms")
@Tag(name = "Admin Device Terms", description = "Quản lý điều khoản thiết bị để đưa vào hợp đồng")
@RequiredArgsConstructor
public class AdminDeviceContractTermController {

    private final DeviceContractTermService deviceContractTermService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo điều khoản cho thiết bị hoặc loại thiết bị")
    public ResponseEntity<?> create(@Valid @RequestBody DeviceContractTermRequestDto request,
                                    Principal principal) {
        Long adminId = resolveAdminId(principal);
        DeviceContractTermResponseDto response = deviceContractTermService.create(request, adminId);
        return ResponseUtil.createSuccessResponse(
                "Tạo điều khoản thành công",
                "Điều khoản đã được lưu",
                response,
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{termId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật điều khoản theo id")
    public ResponseEntity<?> update(@PathVariable Long termId,
                                    @Valid @RequestBody DeviceContractTermRequestDto request,
                                    Principal principal) {
        Long adminId = resolveAdminId(principal);
        DeviceContractTermResponseDto response = deviceContractTermService.update(termId, request, adminId);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật thành công",
                "Điều khoản đã được cập nhật",
                response,
                HttpStatus.OK
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách điều khoản", description = "Có thể lọc theo deviceId, deviceCategoryId, active")
    public ResponseEntity<?> list(@RequestParam(required = false) Long deviceId,
                                  @RequestParam(required = false) Long deviceCategoryId,
                                  @RequestParam(required = false) Boolean active) {
        List<DeviceContractTermResponseDto> terms = deviceContractTermService.list(deviceId, deviceCategoryId, active);
        return ResponseUtil.createSuccessResponse(
                "Danh sách điều khoản",
                "Truy vấn thành công",
                terms,
                HttpStatus.OK
        );
    }

    @GetMapping("/{termId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xem chi tiết điều khoản")
    public ResponseEntity<?> get(@PathVariable Long termId) {
        DeviceContractTermResponseDto term = deviceContractTermService.get(termId);
        return ResponseUtil.createSuccessResponse(
                "Chi tiết điều khoản",
                "Truy vấn thành công",
                term,
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{termId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xoá điều khoản")
    public ResponseEntity<?> delete(@PathVariable Long termId) {
        deviceContractTermService.delete(termId);
        return ResponseUtil.createSuccessResponse(
                "Đã xoá điều khoản",
                "Bản ghi đã bị xoá khỏi hệ thống",
                HttpStatus.OK
        );
    }

    private Long resolveAdminId(Principal principal) {
        if (principal == null) {
            return null;
        }
        try {
            return Long.valueOf(principal.getName());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

