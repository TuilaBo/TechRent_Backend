package com.rentaltech.techrental.webapi.customer.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.dto.*;
import com.rentaltech.techrental.webapi.customer.service.CustomerService;
import com.rentaltech.techrental.webapi.operator.service.KYCService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/customers")
@AllArgsConstructor
@Tag(name = "Khách hàng", description = "API quản lý hồ sơ và tác vụ liên quan đến khách hàng")
public class CustomerController {

    private final CustomerService customerService;
    private final KYCService kycService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Danh sách khách hàng", description = "Liệt kê toàn bộ khách hàng trong hệ thống")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách khách hàng")
    })
    public ResponseEntity<?> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        List<CustomerResponseDto> responseDtos = customers.stream()
                .map(CustomerResponseDto::from)
                .toList();
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách khách hàng thành công",
                "Danh sách khách hàng",
                responseDtos,
                HttpStatus.OK
        );
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Chi tiết khách hàng", description = "Tra cứu thông tin khách hàng theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin khách hàng"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy khách hàng")
    })
    public ResponseEntity<?> getCustomerById(@PathVariable Long customerId) {
        Customer customer = customerService.getCustomerByIdOrThrow(customerId);
        return ResponseUtil.createSuccessResponse(
                "Lấy thông tin khách hàng thành công",
                "Chi tiết khách hàng",
                CustomerResponseDto.from(customer),
                HttpStatus.OK
        );
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Hồ sơ của tôi", description = "Khách hàng xem thông tin hồ sơ của chính mình")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin hồ sơ"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập")
    })
    public ResponseEntity<?> getMyProfile(
            @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseUtil.unauthorized();
        }
        Customer customer = customerService.getCustomerByUsernameOrThrow(principal.getUsername());
        return ResponseUtil.createSuccessResponse(
                "Lấy thông tin hồ sơ thành công",
                "Hồ sơ khách hàng hiện tại",
                CustomerResponseDto.from(customer),
                HttpStatus.OK
        );
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Cập nhật hồ sơ cá nhân", description = "Khách hàng chỉnh sửa hồ sơ của bản thân")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật hồ sơ thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập")
    })
    public ResponseEntity<?> updateMyProfile(
            @RequestBody @Valid CustomerUpdateRequestDto request,
            @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseUtil.unauthorized();
        }
        Customer customer = customerService.updateCustomerByUsername(principal.getUsername(), request);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật hồ sơ thành công",
                "Thông tin hồ sơ đã được cập nhật",
                CustomerResponseDto.from(customer),
                HttpStatus.OK
        );
    }

    @PutMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Cập nhật khách hàng", description = "Quản trị viên cập nhật thông tin khách hàng theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thông tin khách hàng thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ")
    })
    public ResponseEntity<?> updateCustomer(
            @PathVariable Long customerId,
            @RequestBody @Valid CustomerUpdateRequestDto request) {
        Customer customer = customerService.updateCustomer(customerId, request);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật khách hàng thành công",
                "Thông tin khách hàng đã được cập nhật",
                CustomerResponseDto.from(customer),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa khách hàng", description = "Xóa khách hàng theo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Xóa khách hàng thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy khách hàng")
    })
    public ResponseEntity<?> deleteCustomer(@PathVariable Long customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseUtil.createSuccessResponse(
                "Xóa khách hàng thành công",
                "Khách hàng đã được xóa",
                HttpStatus.NO_CONTENT
        );
    }

    @PostMapping("/fcm-token")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Lưu FCM token", description = "Lưu token thông báo đẩy của khách hàng hiện tại")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lưu token thành công"),
            @ApiResponse(responseCode = "400", description = "Token không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập")
    })
    public ResponseEntity<?> saveFcmToken(@AuthenticationPrincipal UserDetails principal,
                                          @RequestBody @Valid SaveFcmTokenRequestDto request) {
        if (principal == null) {
            return ResponseUtil.unauthorized();
        }
        customerService.updateCustomerFcmToken(principal.getUsername(), request.getToken());
        return ResponseUtil.createSuccessResponse(
                "Lưu token thành công",
                "Token FCM đã được cập nhật",
                HttpStatus.OK
        );
    }

    // ========== KYC Endpoints ==========

    /**
     * Get my KYC info
     * GET /api/customers/me/kyc
     */
    @GetMapping("/me/kyc")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Xem thông tin KYC của tôi", description = "Trả về thông tin KYC hiện tại của khách hàng đang đăng nhập")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin KYC"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập")
    })
    public ResponseEntity<?> getMyKyc(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseUtil.unauthorized();
        }
        Map<String, Object> data = kycService.getMyKyc(principal.getUsername());
        return ResponseUtil.createSuccessResponse(
                "Lấy thông tin KYC thành công",
                "Thông tin KYC hiện tại",
                data,
                HttpStatus.OK
        );
    }



    /**
     * Upload multiple KYC documents with personal information
     * POST /api/customers/me/kyc/documents/batch
     */
    @PostMapping(value = "/me/kyc/documents/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Tải lên bộ hồ sơ KYC", description = "Khách hàng tải nhiều ảnh giấy tờ tùy thân phục vụ định danh")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tải lên hồ sơ KYC thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu hoặc file tải lên không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Không có quyền truy cập")
    })
    public ResponseEntity<?> uploadKycDocumentsBatch(
            @Parameter(description = "Ảnh CCCD mặt trước", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestPart(value = "front", required = false) MultipartFile front,
            @Parameter(description = "Ảnh CCCD mặt sau", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestPart(value = "back", required = false) MultipartFile back,
            @Parameter(description = "Ảnh selfie", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary")))
            @RequestPart(value = "selfie", required = false) MultipartFile selfie,
            @Parameter(description = "Họ và tên")
            @RequestPart(value = "fullName", required = false) String fullName,
            @Parameter(description = "Số CCCD/CMND/Passport")
            @RequestPart(value = "identificationCode", required = false) String identificationCode,
            @Parameter(description = "Loại giấy tờ")
            @RequestPart(value = "typeOfIdentification", required = false) String typeOfIdentification,
            @Parameter(description = "Ngày sinh (yyyy-MM-dd)")
            @RequestPart(value = "birthday", required = false) String birthday,
            @Parameter(description = "Ngày hết hạn (yyyy-MM-dd)")
            @RequestPart(value = "expirationDate", required = false) String expirationDate,
            @Parameter(description = "Địa chỉ thường trú")
            @RequestPart(value = "permanentAddress", required = false) String permanentAddress,
            @AuthenticationPrincipal UserDetails principal
    ) {
        if (principal == null) {
            return ResponseUtil.unauthorized();
        }
        
        java.time.LocalDate birthdayDate = null;
        java.time.LocalDate expirationDateObj = null;
        
        try {
            if (birthday != null && !birthday.isBlank()) {
                birthdayDate = java.time.LocalDate.parse(birthday);
            }
            if (expirationDate != null && !expirationDate.isBlank()) {
                expirationDateObj = java.time.LocalDate.parse(expirationDate);
            }
        } catch (Exception e) {
            return ResponseUtil.createErrorResponse(
                    "INVALID_DATE_FORMAT",
                    "Định dạng ngày không hợp lệ",
                    "Ngày phải theo định dạng yyyy-MM-dd",
                    HttpStatus.BAD_REQUEST
            );
        }
        
        Map<String, Object> data = kycService.customerUploadDocumentsWithInfo(
                principal.getUsername(), front, back, selfie,
                fullName, identificationCode, typeOfIdentification,
                birthdayDate, expirationDateObj, permanentAddress
        );
        return ResponseUtil.createSuccessResponse(
                "Upload KYC thành công",
                "Đã lưu các ảnh KYC và thông tin",
                data,
                HttpStatus.OK
        );
    }
}

