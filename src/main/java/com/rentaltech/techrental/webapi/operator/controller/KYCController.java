package com.rentaltech.techrental.webapi.operator.controller;

import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.dto.KYCVerificationDto;
import com.rentaltech.techrental.webapi.operator.service.KYCService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final AccountService accountService;

    /**
     * Upload KYC document (RESTful)
     * POST /api/operator/kyc/customers/{customerId}/documents?type=front_cccd
     */
    @PostMapping("/customers/{customerId}/documents")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Upload KYC document")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Upload thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
        @ApiResponse(responseCode = "404", description = "Customer không tồn tại")
    })
    public ResponseEntity<?> uploadDocument(
            @PathVariable Long customerId,
            @RequestParam("type") String documentType,
            @RequestParam("file") MultipartFile file) {
        
        try {
            // Validate document type
            kycService.validateDocumentType(documentType);
            
            // Service handles upload and updates customer
            Customer customer = kycService.uploadDocument(customerId, file, documentType);
            
            Map<String, Object> data = new HashMap<>();
            data.put("customerId", customer.getCustomerId());
            data.put("imageType", documentType);
            data.put("kycStatus", customer.getKycStatus());
            
            return ResponseUtil.createSuccessResponse(
                    "Upload ảnh thành công!",
                    "Ảnh " + documentType + " đã được lưu",
                    data,
                    HttpStatus.OK
            );
            
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "UPLOAD_DOCUMENT_FAILED",
                    "Upload ảnh thất bại",
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    /**
     * Update KYC status (RESTful: PATCH)
     */
    @PatchMapping("/customers/{customerId}")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN') or hasRole('CUSTOMER')")
    @Operation(summary = "Update KYC status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
        @ApiResponse(responseCode = "404", description = "Customer không tồn tại")
    })
    public ResponseEntity<?> updateKYCStatus(
            @PathVariable Long customerId,
            @RequestBody KYCVerificationDto request,
            @AuthenticationPrincipal UserDetails principal) {
        
        try {
            Long operatorId = getAccountIdFromPrincipal(principal);
            Customer customer = kycService.updateKYCStatus(customerId, request, operatorId);
            
            return ResponseUtil.createSuccessResponse(
                    "Cập nhật KYC thành công!",
                    "Trạng thái KYC đã được cập nhật: " + customer.getKycStatus().getDisplayName(),
                    customer,
                    HttpStatus.OK
            );
            
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "UPDATE_KYC_FAILED",
                    "Cập nhật KYC thất bại",
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    /**
     * Get customers pending verification
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
    @Operation(summary = "Get customers pending KYC verification")
    public ResponseEntity<?> getPendingVerification() {
        try {
            List<Customer> customers = kycService.getPendingVerification();
            
            return ResponseUtil.createSuccessResponse(
                    "Lấy danh sách customer chờ xác minh thành công",
                    "Danh sách customer đang chờ verify KYC",
                    customers,
                    HttpStatus.OK
            );
            
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "GET_PENDING_KYC_FAILED",
                    "Lấy danh sách customer chờ xác minh thất bại",
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Get KYC info for customer
     */
    @GetMapping("/customers/{customerId}")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
    @Operation(summary = "Get KYC info for customer")
    public ResponseEntity<?> getCustomerKYC(@PathVariable Long customerId) {
        try {
            Customer customer = kycService.getKYCInfo(customerId);
            
            Map<String, Object> kycInfo = kycService.buildKYCMap(customer);
            
            return ResponseUtil.createSuccessResponse(
                    "Lấy thông tin KYC thành công",
                    "Thông tin KYC của customer",
                    kycInfo,
                    HttpStatus.OK
            );
            
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "GET_KYC_INFO_FAILED",
                    "Lấy thông tin KYC thất bại",
                    e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        }
    }

    /**
     * Get all KYC statuses (for dropdown/select)
     */
    @GetMapping("/statuses")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
    @Operation(summary = "Get all KYC statuses")
    public ResponseEntity<?> getKYCStatuses() {
        try {
            List<Map<String, String>> statuses = kycService.getKYCStatuses();
            
            return ResponseUtil.createSuccessResponse(
                    "Lấy danh sách trạng thái KYC thành công",
                    "Danh sách tất cả các trạng thái KYC có sẵn",
                    statuses,
                    HttpStatus.OK
            );
            
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "GET_KYC_STATUSES_FAILED",
                    "Lấy danh sách trạng thái thất bại",
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
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
