package com.rentaltech.techrental.webapi.operator.controller;

import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.KYCStatus;
import com.rentaltech.techrental.webapi.customer.model.dto.KYCVerificationDto;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operator/kyc")
@PreAuthorize("hasRole('OPERATOR') or hasRole('ADMIN')")
@Tag(name = "KYC Management", description = "APIs để quản lý KYC cho customer")
public class KYCController {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountService accountService;

    /**
     * Upload KYC document
     * RESTful: POST /api/operator/kyc/customers/{customerId}/documents?type=front_cccd
     */
    @PostMapping("/customers/{customerId}/documents")
    @Operation(summary = "Upload KYC document (front_cccd, back_cccd, selfie)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Upload thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
        @ApiResponse(responseCode = "404", description = "Customer không tồn tại")
    })
    public ResponseEntity<?> uploadDocument(
            @PathVariable Long customerId,
            @RequestParam("type") String documentType, // "front_cccd", "back_cccd", "selfie"
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails principal) {
        
        // Validate document type
        if (!documentType.equals("front_cccd") && 
            !documentType.equals("back_cccd") && 
            !documentType.equals("selfie")) {
            return ResponseUtil.createErrorResponse(
                    "INVALID_DOCUMENT_TYPE",
                    "Loại giấy tờ không hợp lệ",
                    "Loại hợp lệ: front_cccd, back_cccd, selfie",
                    HttpStatus.BAD_REQUEST
            );
        }
        
        return handleImageUpload(customerId, file, documentType, principal);
    }

    // Legacy endpoints - DEPRECATED (backward compatibility)
    @PostMapping("/customers/{customerId}/front-cccd")
    @Deprecated
    public ResponseEntity<?> uploadFrontCCCD(
            @PathVariable Long customerId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails principal) {
        return handleImageUpload(customerId, file, "front_cccd", principal);
    }

    @PostMapping("/customers/{customerId}/back-cccd")
    @Deprecated
    public ResponseEntity<?> uploadBackCCCD(
            @PathVariable Long customerId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails principal) {
        return handleImageUpload(customerId, file, "back_cccd", principal);
    }

    @PostMapping("/customers/{customerId}/selfie")
    @Deprecated
    public ResponseEntity<?> uploadSelfie(
            @PathVariable Long customerId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails principal) {
        return handleImageUpload(customerId, file, "selfie", principal);
    }

    /**
     * Upload và lưu ảnh
     * TODO: Implement actual file upload to cloud storage (S3, Azure Blob, etc.)
     */
    private ResponseEntity<?> handleImageUpload(Long customerId, MultipartFile file, String imageType, UserDetails principal) {
        try {
            // Validate customer exists
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer không tồn tại: " + customerId));

            // Validate file
            if (file.isEmpty()) {
                return ResponseUtil.createErrorResponse(
                        "EMPTY_FILE",
                        "File không được để trống",
                        "Vui lòng chọn file ảnh",
                        HttpStatus.BAD_REQUEST
                );
            }

            // TODO: Upload file to cloud storage (S3, Azure Blob, etc.)
            // For now, save as URL placeholder
            String imageUrl = saveImageAndReturnUrl(file, customerId, imageType);

            // Update customer with image URL
            switch (imageType) {
                case "front_cccd" -> customer.setKycFrontCCCDUrl(imageUrl);
                case "back_cccd" -> customer.setKycBackCCCDUrl(imageUrl);
                case "selfie" -> customer.setKycSelfieUrl(imageUrl);
            }

            // Update KYC status
            updateKYCStatusAfterUpload(customer);
            
            customerRepository.save(customer);

            Map<String, Object> data = new HashMap<>();
            data.put("imageUrl", imageUrl);
            data.put("imageType", imageType);
            data.put("kycStatus", customer.getKycStatus());

            return ResponseUtil.createSuccessResponse(
                    "Upload ảnh thành công!",
                    "Ảnh " + imageType + " đã được lưu",
                    data,
                    HttpStatus.OK
            );

        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "UPLOAD_IMAGE_FAILED",
                    "Upload ảnh thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    /**
     * Save image to storage and return URL
     * TODO: Implement with cloud storage
     */
    private String saveImageAndReturnUrl(MultipartFile file, Long customerId, String imageType) {
        // TODO: Implement actual file upload
        // For now, return mock URL
        return String.format("/uploads/kyc/customer_%d_%s_%d.jpg", 
                customerId, imageType, System.currentTimeMillis());
    }

    /**
     * Update KYC status based on uploaded documents
     */
    private void updateKYCStatusAfterUpload(Customer customer) {
        boolean hasFront = customer.getKycFrontCCCDUrl() != null;
        boolean hasBack = customer.getKycBackCCCDUrl() != null;
        boolean hasSelfie = customer.getKycSelfieUrl() != null;

        if (hasFront && hasBack && hasSelfie) {
            customer.setKycStatus(KYCStatus.DOCUMENTS_SUBMITTED);
        } else if (hasFront || hasBack || hasSelfie) {
            customer.setKycStatus(KYCStatus.PENDING_VERIFICATION);
        }
    }

    /**
     * Update KYC status (RESTful: PATCH resource)
     */
    @PatchMapping("/customers/{customerId}")
    @Operation(summary = "Update KYC status (RESTful)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
        @ApiResponse(responseCode = "404", description = "Customer không tồn tại")
    })
    public ResponseEntity<?> updateKYC(
            @PathVariable Long customerId,
            @RequestBody KYCVerificationDto request,
            @AuthenticationPrincipal UserDetails principal) {
        return verifyKYC(customerId, request, principal);
    }

    /**
     * Legacy endpoint - DEPRECATED
     */
    @PostMapping("/customers/{customerId}/verify")
    @Deprecated
    public ResponseEntity<?> verifyKYCLegacy(
            @PathVariable Long customerId,
            @RequestBody KYCVerificationDto request,
            @AuthenticationPrincipal UserDetails principal) {
        return verifyKYC(customerId, request, principal);
    }

    /**
     * Helper method for KYC verification
     */
    private ResponseEntity<?> verifyKYC(
            Long customerId,
            KYCVerificationDto request,
            UserDetails principal) {
        
        try {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer không tồn tại: " + customerId));

            Long operatorId = getAccountIdFromPrincipal(principal);

            customer.setKycStatus(request.getStatus());
            customer.setKycVerifiedAt(LocalDateTime.now());
            customer.setKycVerifiedBy(operatorId);

            if (request.getStatus() == KYCStatus.REJECTED) {
                customer.setKycRejectionReason(request.getRejectionReason());
            }

            customerRepository.save(customer);

            return ResponseUtil.createSuccessResponse(
                    "Xác minh KYC thành công!",
                    "Trạng thái KYC của customer đã được cập nhật: " + request.getStatus().getDisplayName(),
                    customer,
                    HttpStatus.OK
            );

        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "VERIFY_KYC_FAILED",
                    "Xác minh KYC thất bại",
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    /**
     * Xem danh sách customer cần verify KYC
     */
    @GetMapping("/pending-verification")
    @Operation(summary = "Xem danh sách customer chờ xác minh KYC")
    public ResponseEntity<?> getPendingVerification() {
        try {
            List<Customer> customers = customerRepository.findByKycStatus(KYCStatus.PENDING_VERIFICATION);
            customers.addAll(customerRepository.findByKycStatus(KYCStatus.DOCUMENTS_SUBMITTED));

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
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Xem thông tin KYC của customer
     */
    @GetMapping("/customers/{customerId}")
    @Operation(summary = "Xem thông tin KYC của customer")
    public ResponseEntity<?> getCustomerKYC(@PathVariable Long customerId) {
        try {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer không tồn tại: " + customerId));

            Map<String, Object> kycInfo = new HashMap<>();
            kycInfo.put("customerId", customer.getCustomerId());
            kycInfo.put("fullName", customer.getFullName());
            kycInfo.put("kycStatus", customer.getKycStatus());
            kycInfo.put("frontCCCDUrl", customer.getKycFrontCCCDUrl());
            kycInfo.put("backCCCDUrl", customer.getKycBackCCCDUrl());
            kycInfo.put("selfieUrl", customer.getKycSelfieUrl());
            kycInfo.put("verifiedAt", customer.getKycVerifiedAt());
            kycInfo.put("verifiedBy", customer.getKycVerifiedBy());
            kycInfo.put("rejectionReason", customer.getKycRejectionReason());

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
                    "Có lỗi xảy ra: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

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

