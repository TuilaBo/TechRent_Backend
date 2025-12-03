package com.rentaltech.techrental.webapi.admin.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.policy.model.dto.PolicyRequestDto;
import com.rentaltech.techrental.policy.model.dto.PolicyResponseDto;
import com.rentaltech.techrental.policy.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/policies")
@RequiredArgsConstructor
@Tag(name = "Policy Management", description = "Quản lý chính sách hệ thống")
public class AdminPolicyController {

    private final PolicyService policyService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo policy mới", description = "Admin có thể tạo policy mới với file PDF hoặc Word")
    public ResponseEntity<?> create(
            @RequestPart("request") @Valid PolicyRequestDto request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Authentication authentication) {
        PolicyResponseDto response = policyService.create(
                request,
                file,
                authentication != null ? authentication.getName() : null
        );
        return ResponseUtil.createSuccessResponse(
                "Tạo policy thành công",
                "Policy đã được tạo",
                response,
                HttpStatus.CREATED
        );
    }

    @PutMapping(value = "/{policyId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật policy", description = "Admin có thể cập nhật thông tin policy và file")
    public ResponseEntity<?> update(
            @PathVariable Long policyId,
            @RequestPart("request") @Valid PolicyRequestDto request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Authentication authentication) {
        PolicyResponseDto response = policyService.update(
                policyId,
                request,
                file,
                authentication != null ? authentication.getName() : null
        );
        return ResponseUtil.createSuccessResponse(
                "Cập nhật policy thành công",
                "Policy đã được cập nhật",
                response,
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{policyId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa policy", description = "Admin có thể xóa policy (soft delete)")
    public ResponseEntity<?> delete(
            @PathVariable Long policyId,
            Authentication authentication) {
        policyService.delete(policyId, authentication != null ? authentication.getName() : null);
        return ResponseUtil.createSuccessResponse(
                "Xóa policy thành công",
                "Policy đã được xóa",
                null,
                HttpStatus.OK
        );
    }

    @GetMapping("/{policyId}")
    @Operation(summary = "Xem chi tiết policy", description = "Ai cũng có quyền xem chi tiết policy")
    public ResponseEntity<?> getById(@PathVariable Long policyId) {
        PolicyResponseDto response = policyService.getById(policyId);
        return ResponseUtil.createSuccessResponse(
                "Chi tiết policy",
                "",
                response,
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "Danh sách tất cả policy", description = "Ai cũng có quyền xem danh sách policy")
    public ResponseEntity<?> getAll() {
        List<PolicyResponseDto> responses = policyService.getAll();
        return ResponseUtil.createSuccessResponse(
                "Danh sách policy",
                "Có " + responses.size() + " policy",
                responses,
                HttpStatus.OK
        );
    }

    @GetMapping("/active")
    @Operation(summary = "Danh sách policy đang hiệu lực", description = "Ai cũng có quyền xem policy đang hiệu lực")
    public ResponseEntity<?> getActivePolicies() {
        List<PolicyResponseDto> responses = policyService.getActivePolicies();
        return ResponseUtil.createSuccessResponse(
                "Danh sách policy đang hiệu lực",
                "Có " + responses.size() + " policy đang hiệu lực",
                responses,
                HttpStatus.OK
        );
    }
}

