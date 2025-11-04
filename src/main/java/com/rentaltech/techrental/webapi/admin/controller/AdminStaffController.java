package com.rentaltech.techrental.webapi.admin.controller;

import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.dto.AdminStaffCreateWithAccountRequestDto;
import com.rentaltech.techrental.staff.model.dto.StaffResponseDto;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/staff")
@Tag(name = "Admin Staff", description = "Admin staff management APIs")
public class AdminStaffController {

    @Autowired
    private StaffService staffService;

    
    // Tạo staff + account trong 1 call (Admin only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create staff with account", description = "Admin nhập email/password/role -> tạo cả Account và Staff")
    public ResponseEntity<StaffResponseDto> createStaffWithAccount(@RequestBody @Valid AdminStaffCreateWithAccountRequestDto request) {
        try {
            Staff savedStaff = staffService.createStaffWithAccount(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponseDto(savedStaff));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // Lấy tất cả staff (Admin only)
    @GetMapping
    @Operation(summary = "List staff", description = "Retrieve all staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StaffResponseDto>> getAllStaff() {
        List<Staff> staffList = staffService.getAllStaff();
        List<StaffResponseDto> responseDtos = staffList.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    // Lấy staff theo ID
    @GetMapping("/{staffId}")
    @Operation(summary = "Get staff by ID", description = "Retrieve staff by ID")
    public ResponseEntity<StaffResponseDto> getStaffById(@PathVariable Long staffId) {
        return staffService.getStaffById(staffId)
                .map(staff -> ResponseEntity.ok(mapToResponseDto(staff)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Lấy staff theo Account ID
    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get staff by account ID", description = "Retrieve staff by account ID")
    public ResponseEntity<StaffResponseDto> getStaffByAccountId(@PathVariable Long accountId) {
        return staffService.getStaffByAccountId(accountId)
                .map(staff -> ResponseEntity.ok(mapToResponseDto(staff)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Lấy staff theo role
    @GetMapping("/role/{staffRole}")
    @Operation(summary = "Get staff by role", description = "List staff by role")
    public ResponseEntity<List<StaffResponseDto>> getStaffByRole(@PathVariable StaffRole staffRole) {
        List<Staff> staffList = staffService.getStaffByRole(staffRole);
        List<StaffResponseDto> responseDtos = staffList.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    // Lấy active staff
    @GetMapping("/active")
    @Operation(summary = "Get active staff", description = "List active staff members")
    public ResponseEntity<List<StaffResponseDto>> getActiveStaff() {
        List<Staff> activeStaff = staffService.getActiveStaff();
        List<StaffResponseDto> responseDtos = activeStaff.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    // Cập nhật trạng thái active/inactive (Admin only)
    @PutMapping("/{staffId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update staff status", description = "Activate or deactivate a staff member")
    public ResponseEntity<StaffResponseDto> updateStaffStatus(@PathVariable Long staffId, 
                                                             @RequestParam Boolean isActive) {
        try {
            Staff updatedStaff = staffService.updateStaffStatus(staffId, isActive);
            return ResponseEntity.ok(mapToResponseDto(updatedStaff));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Cập nhật staff role (Admin only)
    @PutMapping("/{staffId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update staff role", description = "Change a staff member's role")
    public ResponseEntity<StaffResponseDto> updateStaffRole(@PathVariable Long staffId, 
                                                           @RequestParam StaffRole staffRole) {
        try {
            Staff updatedStaff = staffService.updateStaffRole(staffId, staffRole);
            return ResponseEntity.ok(mapToResponseDto(updatedStaff));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Xóa staff (Admin only - soft delete)
    @DeleteMapping("/{staffId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete staff", description = "Soft delete a staff member by ID")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long staffId) {
        try {
            staffService.deleteStaff(staffId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
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
