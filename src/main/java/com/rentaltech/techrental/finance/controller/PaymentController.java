package com.rentaltech.techrental.finance.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.finance.model.dto.CreatePaymentRequest;
import com.rentaltech.techrental.finance.model.dto.CreatePaymentResponse;
import com.rentaltech.techrental.finance.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<?> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        CreatePaymentResponse response = paymentService.createPayment(request);
        return ResponseUtil.createSuccessResponse(
                "Payment created successfully",
                "Link to proceed with payment",
                response,
                HttpStatus.CREATED
        );
    }

    @GetMapping("/invoice/{rentalOrderId}")
    public ResponseEntity<?> getInvoiceForOrder(
            @PathVariable Long rentalOrderId,
            @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseUtil.createSuccessResponse(
                "Invoice retrieved successfully",
                "Invoice details for rental order " + rentalOrderId,
                paymentService.getInvoiceForCustomer(rentalOrderId, principal.getUsername()),
                HttpStatus.OK
        );
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions() {
        return ResponseUtil.createSuccessResponse(
                "Transactions retrieved successfully",
                "List of recorded payment transactions",
                paymentService.getAllTransactions(),
                HttpStatus.OK
        );
    }

    @PostMapping("/settlements/{settlementId}/confirm-refund")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    public ResponseEntity<?> confirmDepositRefund(@PathVariable Long settlementId, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        return ResponseUtil.createSuccessResponse(
                "Hoàn cọc thành công",
                "Đã xác nhận hoàn cọc cho settlement",
                paymentService.confirmDepositRefund(settlementId, username),
                HttpStatus.OK
        );
    }
}
