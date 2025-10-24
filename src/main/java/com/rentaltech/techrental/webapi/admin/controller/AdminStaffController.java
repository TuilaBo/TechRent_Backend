package com.rentaltech.techrental.webapi.admin.controller;

import com.rentaltech.techrental.staff.model.Staff;
import com.rentaltech.techrental.staff.model.StaffRole;
import com.rentaltech.techrental.staff.model.dto.StaffCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.StaffResponseDto;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/staff")
public class AdminStaffController {

    @Autowired
    private StaffService staffService;

    // Tạo staff mới (Admin only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffResponseDto> createStaff(@RequestBody @Valid StaffCreateRequestDto request) {
        try {
            Staff savedStaff = staffService.createStaff(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponseDto(savedStaff));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // Lấy tất cả staff (Admin only)
    @GetMapping
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
    public ResponseEntity<StaffResponseDto> getStaffById(@PathVariable Long staffId) {
        return staffService.getStaffById(staffId)
                .map(staff -> ResponseEntity.ok(mapToResponseDto(staff)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Lấy staff theo Account ID
    @GetMapping("/account/{accountId}")
    public ResponseEntity<StaffResponseDto> getStaffByAccountId(@PathVariable Long accountId) {
        return staffService.getStaffByAccountId(accountId)
                .map(staff -> ResponseEntity.ok(mapToResponseDto(staff)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Lấy staff theo role
    @GetMapping("/role/{staffRole}")
    public ResponseEntity<List<StaffResponseDto>> getStaffByRole(@PathVariable StaffRole staffRole) {
        List<Staff> staffList = staffService.getStaffByRole(staffRole);
        List<StaffResponseDto> responseDtos = staffList.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    // Lấy active staff
    @GetMapping("/active")
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
