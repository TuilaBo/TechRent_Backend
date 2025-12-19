package com.rentaltech.techrental.webapi.admin.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.policy.model.dto.PolicyRequestDto;
import com.rentaltech.techrental.policy.model.dto.PolicyResponseDto;
import com.rentaltech.techrental.policy.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
            @RequestPart("request") PolicyRequestDto request,
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
            @RequestPart("request") PolicyRequestDto request,
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

    @GetMapping("/{policyId}/file")
    @Operation(summary = "Xem file policy (PDF)", description = "Trả về file PDF để browser mở trực tiếp với tên file đẹp")
    public ResponseEntity<byte[]> viewPolicyFile(@PathVariable Long policyId) throws IOException {
        PolicyResponseDto policy = policyService.getById(policyId);
        if (policy.getFileUrl() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        URL url = new URL(policy.getFileUrl());
        byte[] bytes = url.openStream().readAllBytes();

        String filename = policy.getFileName() != null && !policy.getFileName().isBlank()
                ? policy.getFileName()
                : "policy-" + policyId + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.inline()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
        );

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{policyId}/download")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tải file gốc policy (Word/DOCX)", description = "Admin có thể tải file gốc với tên file đẹp")
    public ResponseEntity<byte[]> downloadOriginalFile(@PathVariable Long policyId) throws IOException {
        PolicyResponseDto policy = policyService.getById(policyId);
        if (policy.getOriginalFileUrl() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        URL url = new URL(policy.getOriginalFileUrl());
        byte[] bytes = url.openStream().readAllBytes();

        // Parse extension từ originalFileUrl để xác định đúng loại file
        String originalUrl = policy.getOriginalFileUrl();
        String extension = extractExtensionFromUrl(originalUrl);
        String fileTypeFromUrl = extension.toUpperCase();

        // Tạo filename: nếu fileName có .pdf thì bỏ đi, thêm extension đúng
        String filename;
        if (policy.getFileName() != null && !policy.getFileName().isBlank()) {
            String baseName = policy.getFileName().replaceAll("\\.pdf$", "").replaceAll("\\.(doc|docx)$", "");
            filename = baseName + "." + extension;
        } else {
            filename = "policy-" + policyId + "." + extension;
        }

        MediaType contentType = switch (fileTypeFromUrl) {
            case "DOC" -> MediaType.parseMediaType("application/msword");
            case "DOCX" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "PDF" -> MediaType.APPLICATION_PDF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
        );

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    private String extractExtensionFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return "docx";
        }
        // Lấy phần sau dấu chấm cuối cùng trong URL (trước dấu ? nếu có)
        String path = url.split("\\?")[0];
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0 && lastDot < path.length() - 1) {
            return path.substring(lastDot + 1).toLowerCase();
        }
        return "docx"; // default
    }
}

