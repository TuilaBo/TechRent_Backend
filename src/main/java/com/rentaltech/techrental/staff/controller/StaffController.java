package com.rentaltech.techrental.staff.controller;

import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.dto.StaffResponseDto;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import com.rentaltech.techrental.common.util.ResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/staff")
@Tag(name = "Staff", description = "Staff read APIs")
public class StaffController {

    @Autowired
    private StaffService staffService;

    // Lấy staff theo ID
    @GetMapping("/{staffId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
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

    // Lấy staff theo role (chỉ active staff)
    @GetMapping("/role/{staffRole}")
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
