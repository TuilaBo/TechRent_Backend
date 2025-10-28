package com.rentaltech.techrental.webapi.operator.controller;

import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.dto.KYCVerificationDto;
import com.rentaltech.techrental.webapi.operator.service.KYCService;
import com.rentaltech.techrental.webapi.operator.service.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operator/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC Management", description = "APIs để quản lý KYC cho customer")
public class KYCController {

    private final KYCService kycService;
    private final OcrService ocrService;
    private final AccountService accountService;
    



    /**
     * Customer upload KYC document (current user)
     * POST /api/customers/me/kyc/documents?type=front_cccd|back_cccd|selfie
     */
    @PostMapping(path = "/api/customers/me/kyc/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Customer upload KYC document (me)")
    public ResponseEntity<?> uploadMyKycDocument(
            @RequestParam("type") String documentType,
            @Parameter(description = "File ảnh KYC", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Map<String, Object> data = kycService.customerUploadDocument(principal.getUsername(), file, documentType);
        return ResponseUtil.createSuccessResponse(
                "Upload KYC thành công",
                "Ảnh " + documentType + " đã được lưu",
                data,
                HttpStatus.OK
        );
    }

    /**
     * Customer upload 1 lần nhiều ảnh KYC (front/back/selfie)
     * POST /api/customers/me/kyc/documents/batch
     */
    @PostMapping(path = "/api/customers/me/kyc/documents/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Customer upload nhiều ảnh KYC (me)")
    public ResponseEntity<?> uploadMyKycDocumentsBatch(
            @Parameter(description = "Ảnh CCCD mặt trước", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestPart(value = "front", required = false) MultipartFile front,
            @Parameter(description = "Ảnh CCCD mặt sau", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestPart(value = "back", required = false) MultipartFile back,
            @Parameter(description = "Ảnh selfie", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestPart(value = "selfie", required = false) MultipartFile selfie,
            @AuthenticationPrincipal UserDetails principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Map<String, Object> data = kycService.customerUploadDocuments(principal.getUsername(), front, back, selfie);
        return ResponseUtil.createSuccessResponse(
                "Upload KYC nhiều ảnh thành công",
                "Đã lưu các ảnh KYC được chọn",
                data,
                HttpStatus.OK
        );
    }

    /**
     * OCR CCCD image to extract text (front/back/selfie)
     * POST /api/operator/kyc/ocr?lang=vie
     */
    @PostMapping("/ocr")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
    @Operation(summary = "OCR CCCD image to text (Tesseract)")
    public ResponseEntity<?> ocrCccd(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "lang", required = false) String lang
    ) {
        String text = ocrService.extractText(file, lang);
        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        data.put("language", (lang == null || lang.isBlank()) ? "eng" : lang);
        return ResponseUtil.createSuccessResponse(
                "OCR thành công",
                "Đã trích xuất văn bản từ ảnh",
                data,
                HttpStatus.OK
        );
    }

    /**
     * Update KYC status (RESTful: PATCH)
     */
    @PatchMapping("/customers/{customerId}")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
    @Operation(summary = "Update KYC status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
        @ApiResponse(responseCode = "404", description = "Customer không tồn tại")
    })
    public ResponseEntity<?> updateKYCStatus(
            @PathVariable Long customerId,
            @RequestBody KYCVerificationDto request,
            @AuthenticationPrincipal UserDetails principal) {
        Long operatorId = getAccountIdFromPrincipal(principal);
        Map<String, Object> kycInfo = kycService.updateKycStatusAndBuild(customerId, request, operatorId);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật KYC thành công!",
                "Đã cập nhật trạng thái KYC",
                kycInfo,
                HttpStatus.OK
        );
    }

    /**
     * Get customers pending verification
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
    @Operation(summary = "Get customers pending KYC verification")
    public ResponseEntity<?> getPendingVerification() {
        List<Map<String, Object>> data = kycService.getPendingVerificationMaps();
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách customer chờ xác minh thành công",
                "Danh sách customer đang chờ verify KYC",
                data,
                HttpStatus.OK
        );
    }

    /**
     * Get KYC info for customer
     */
    @GetMapping("/customers/{customerId}")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
    @Operation(summary = "Get KYC info for customer")
    public ResponseEntity<?> getCustomerKYC(@PathVariable Long customerId) {
        Customer customer = kycService.getKYCInfo(customerId);
        Map<String, Object> kycInfo = kycService.buildKYCMap(customer);
        return ResponseUtil.createSuccessResponse(
                "Lấy thông tin KYC thành công",
                "Thông tin KYC của customer",
                kycInfo,
                HttpStatus.OK
        );
    }

    /**
     * Get all KYC statuses (for dropdown/select)
     */
    @GetMapping("/statuses")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
    @Operation(summary = "Get all KYC statuses")
    public ResponseEntity<?> getKYCStatuses() {
        List<Map<String, String>> statuses = kycService.getKYCStatuses();
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách trạng thái KYC thành công",
                "Danh sách tất cả các trạng thái KYC có sẵn",
                statuses,
                HttpStatus.OK
        );
    }

    /**
     * Customer xem trạng thái KYC của chính mình
     * GET /api/customers/me/kyc
     */
    @GetMapping("/api/customers/me/kyc")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my KYC info (me)")
    public ResponseEntity<?> getMyKyc(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Map<String, Object> data = kycService.getMyKyc(principal.getUsername());
        return ResponseUtil.createSuccessResponse(
                "Lấy thông tin KYC của tôi thành công",
                "Thông tin KYC hiện tại",
                data,
                HttpStatus.OK
        );
    }

    

    // Helper methods
    
    private Long getAccountIdFromPrincipal(UserDetails principal) {
        try {
            String username = principal.getUsername();
            return accountService.getByUsername(username).orElseThrow(
                () -> new RuntimeException("Không tìm thấy tài khoản với username: " + username)
            ).getAccountId();
        } catch (Exception e) {
            throw new RuntimeException("Không thể lấy thông tin tài khoản: " + e.getMessage());
        }
    }
}
