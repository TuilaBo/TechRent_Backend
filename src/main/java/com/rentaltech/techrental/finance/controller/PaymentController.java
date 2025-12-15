package com.rentaltech.techrental.finance.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.finance.model.dto.CreatePaymentRequest;
import com.rentaltech.techrental.finance.model.dto.CreatePaymentResponse;
import com.rentaltech.techrental.finance.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "Quản lý thanh toán", description = "Các API tạo giao dịch, xem hóa đơn và xử lý hoàn cọc")
public class    PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Tạo giao dịch thanh toán", description = "Sinh link thanh toán cho đơn thuê hoặc hóa đơn")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo giao dịch thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Không thể tạo giao dịch do lỗi hệ thống")
    })
    public ResponseEntity<?> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        CreatePaymentResponse response = paymentService.createPayment(request);
        return ResponseUtil.createSuccessResponse(
                "Tạo giao dịch thành công",
                "Link thanh toán cho giao dịch",
                response,
                HttpStatus.CREATED
        );
    }

    @GetMapping("/invoice/{rentalOrderId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF') or hasRole('CUSTOMER')")
    @Operation(summary = "Lấy hóa đơn của đơn thuê", description = "Khách hàng xem thông tin hóa đơn gắn với đơn thuê")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thông tin hóa đơn"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy hóa đơn hoặc không thuộc sở hữu"),
            @ApiResponse(responseCode = "500", description = "Không thể lấy hóa đơn do lỗi hệ thống")
    })
    public ResponseEntity<?> getInvoiceForOrder(
            @PathVariable Long rentalOrderId,
            @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseUtil.createSuccessResponse(
                "Lấy hóa đơn thành công",
                "Danh sách hóa đơn của đơn thuê " + rentalOrderId,
                paymentService.getInvoiceForCustomer(rentalOrderId, principal.getUsername()),
                HttpStatus.OK
        );
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Danh sách giao dịch", description = "Xem toàn bộ giao dịch đã ghi nhận")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách giao dịch"),
            @ApiResponse(responseCode = "500", description = "Không thể lấy danh sách do lỗi hệ thống")
    })
    public ResponseEntity<?> getAllTransactions() {
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách giao dịch thành công",
                "Danh sách các giao dịch đã ghi nhận",
                paymentService.getAllTransactions(),
                HttpStatus.OK
        );
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách hóa đơn", description = "Admin xem toàn bộ hóa đơn đã phát hành")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về danh sách hóa đơn"),
            @ApiResponse(responseCode = "500", description = "Không thể lấy danh sách do lỗi hệ thống")
    })
    public ResponseEntity<?> getAllInvoices() {
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách hóa đơn thành công",
                "Toàn bộ hóa đơn đã phát hành",
                paymentService.getAllInvoices(),
                HttpStatus.OK
        );
    }

    @PostMapping(value = "/settlements/{settlementId}/confirm-refund", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Xác nhận hoàn cọc", description = "Nhân viên xác nhận hoàn cọc cho một settlement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hoàn cọc thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc chưa đủ điều kiện hoàn"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền xác nhận hoàn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy settlement"),
            @ApiResponse(responseCode = "500", description = "Không thể xử lý yêu cầu do lỗi hệ thống")
    })
    public ResponseEntity<?> confirmDepositRefund(@PathVariable Long settlementId,
                                                  @RequestPart("proof") MultipartFile proofFile,
                                                  Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        return ResponseUtil.createSuccessResponse(
                "Hoàn cọc thành công",
                "Đã xác nhận hoàn cọc cho settlement",
                paymentService.confirmDepositRefund(settlementId, username, proofFile),
                HttpStatus.OK
        );
    }
}
