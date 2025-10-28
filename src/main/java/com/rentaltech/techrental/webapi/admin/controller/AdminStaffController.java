package com.rentaltech.techrental.webapi.admin.controller;

import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.dto.StaffCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.StaffResponseDto;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import com.rentaltech.techrental.common.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/admin/staff")
@Tag(name = "Admin Staff", description = "Admin staff management APIs")
public class AdminStaffController {

    @Autowired
    private StaffService staffService;

    // Tạo staff mới (Admin only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create staff", description = "Create a new staff account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<?> createStaff(@RequestBody @Valid StaffCreateRequestDto request) {
        Staff savedStaff = staffService.createStaff(request);
        return ResponseUtil.createSuccessResponse(
                "Tạo nhân viên thành công",
                "Nhân viên đã được tạo",
                mapToResponseDto(savedStaff),
                HttpStatus.CREATED
        );
    }

    // Lấy tất cả staff (Admin only)
    @GetMapping
    @Operation(summary = "List staff", description = "Retrieve all staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllStaff() {
        List<Staff> staffList = staffService.getAllStaff();
        List<StaffResponseDto> responseDtos = staffList.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách nhân viên thành công",
                "Danh sách nhân viên",
                responseDtos,
                HttpStatus.OK
        );
    }

    // Lấy staff theo ID
    @GetMapping("/{staffId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get staff by ID", description = "Retrieve staff by ID")
    public ResponseEntity<?> getStaffById(@PathVariable Long staffId) {
        Staff staff = staffService.getStaffByIdOrThrow(staffId);
        return ResponseUtil.createSuccessResponse(
                "Lấy nhân viên thành công",
                "Chi tiết nhân viên",
                mapToResponseDto(staff),
                HttpStatus.OK
        );
    }

    // Lấy staff theo Account ID
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get staff by account ID", description = "Retrieve staff by account ID")
    public ResponseEntity<?> getStaffByAccountId(@PathVariable Long accountId) {
        Staff staff = staffService.getStaffByAccountIdOrThrow(accountId);
        return ResponseUtil.createSuccessResponse(
                "Lấy nhân viên theo tài khoản thành công",
                "Chi tiết nhân viên",
                mapToResponseDto(staff),
                HttpStatus.OK
        );
    }

    // Lấy staff theo role
    @GetMapping("/role/{staffRole}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get staff by role", description = "List staff by role")
    public ResponseEntity<?> getStaffByRole(@PathVariable StaffRole staffRole) {
        List<Staff> staffList = staffService.getStaffByRole(staffRole);
        List<StaffResponseDto> responseDtos = staffList.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách nhân viên theo vai trò thành công",
                "Danh sách nhân viên theo vai trò",
                responseDtos,
                HttpStatus.OK
        );
    }

    // Lấy active staff
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get active staff", description = "List active staff members")
    public ResponseEntity<?> getActiveStaff() {
        List<Staff> activeStaff = staffService.getActiveStaff();
        List<StaffResponseDto> responseDtos = activeStaff.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách nhân viên đang hoạt động thành công",
                "Danh sách nhân viên đang hoạt động",
                responseDtos,
                HttpStatus.OK
        );
    }

    // Cập nhật trạng thái active/inactive (Admin only)
    @PutMapping("/{staffId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update staff status", description = "Activate or deactivate a staff member")
    public ResponseEntity<?> updateStaffStatus(@PathVariable Long staffId,
                                               @RequestParam Boolean isActive) {
        Staff updatedStaff = staffService.updateStaffStatus(staffId, isActive);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật trạng thái nhân viên thành công",
                "Trạng thái nhân viên đã được cập nhật",
                mapToResponseDto(updatedStaff),
                HttpStatus.OK
        );
    }

    // Cập nhật staff role (Admin only)
    @PutMapping("/{staffId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update staff role", description = "Change a staff member's role")
    public ResponseEntity<?> updateStaffRole(@PathVariable Long staffId,
                                             @RequestParam StaffRole staffRole) {
        Staff updatedStaff = staffService.updateStaffRole(staffId, staffRole);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật vai trò nhân viên thành công",
                "Vai trò nhân viên đã được cập nhật",
                mapToResponseDto(updatedStaff),
                HttpStatus.OK
        );
    }

    // Xóa staff (Admin only - soft delete)
    @DeleteMapping("/{staffId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete staff", description = "Soft delete a staff member by ID")
    public ResponseEntity<?> deleteStaff(@PathVariable Long staffId) {
        staffService.deleteStaff(staffId);
        return ResponseUtil.createSuccessResponse(
                "Xóa nhân viên thành công",
                "Nhân viên đã được xóa (soft delete)",
                HttpStatus.NO_CONTENT
        );
    }

    private StaffResponseDto mapToResponseDto(Staff staff) {
        return StaffResponseDto.builder()
                .staffId(staff.getStaffId())
                .accountId(staff.getAccount().getAccountId())
                .username(staff.getAccount().getUsername())
                .email(staff.getAccount().getEmail())
                .phoneNumber(staff.getAccount().getPhoneNumber())
                .staffRole(staff.getStaffRole())
                .isActive(staff.getIsActive())
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .build();
    }
}
