package com.rentaltech.techrental.webapi.customer.controller;

import com.rentaltech.techrental.common.util.ResponseUtil;
import com.rentaltech.techrental.contract.model.dto.EmailPinRequestDto;
import com.rentaltech.techrental.staff.model.dto.HandoverPinDeliveryDto;
import com.rentaltech.techrental.staff.model.dto.HandoverReportCustomerSignRequestDto;
import com.rentaltech.techrental.staff.model.dto.HandoverReportResponseDto;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.staff.service.handover.HandoverReportService;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers/handover-reports")
@RequiredArgsConstructor
@Slf4j
public class CustomerHandoverReportController {

    private final HandoverReportService handoverReportService;
    private final CustomerService customerService;
    private final RentalOrderRepository rentalOrderRepository;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMyHandoverReports(@AuthenticationPrincipal UserDetails principal) {
        Customer customer = getCustomerFromPrincipal(principal);
        List<HandoverReportResponseDto> reports = handoverReportService.getReportsByCustomerOrder(customer.getCustomerId());
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách biên bản bàn giao thành công",
                "Danh sách biên bản bàn giao của các đơn hàng của bạn",
                reports,
                HttpStatus.OK
        );
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getHandoverReportsByOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails principal) {
        Customer customer = getCustomerFromPrincipal(principal);
        
        // Verify order belongs to customer
        boolean orderBelongsToCustomer = rentalOrderRepository.findById(orderId)
                .map(order -> order.getCustomer() != null && 
                             order.getCustomer().getCustomerId().equals(customer.getCustomerId()))
                .orElse(false);
        
        if (!orderBelongsToCustomer) {
            return ResponseUtil.createErrorResponse(
                    "FORBIDDEN",
                    "Không có quyền truy cập",
                    "Đơn hàng không thuộc về bạn",
                    HttpStatus.FORBIDDEN
            );
        }
        
        List<HandoverReportResponseDto> reports = handoverReportService.getReportsByOrder(orderId);
        
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách biên bản bàn giao thành công",
                "Danh sách biên bản bàn giao của đơn hàng " + orderId,
                reports,
                HttpStatus.OK
        );
    }

    @GetMapping("/{handoverReportId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getHandoverReport(@PathVariable Long handoverReportId, @AuthenticationPrincipal UserDetails principal) {
        Customer customer = getCustomerFromPrincipal(principal);
        HandoverReportResponseDto report = handoverReportService.getReport(handoverReportId);
        
        // Verify the report belongs to customer's order
        if (report.getOrderId() == null) {
            return ResponseUtil.createErrorResponse(
                    "INVALID_REPORT",
                    "Biên bản không hợp lệ",
                    "Biên bản không có thông tin đơn hàng",
                    HttpStatus.BAD_REQUEST
            );
        }
        
        // Verify order belongs to customer
        boolean orderBelongsToCustomer = rentalOrderRepository.findById(report.getOrderId())
                .map(order -> order.getCustomer() != null && 
                             order.getCustomer().getCustomerId().equals(customer.getCustomerId()))
                .orElse(false);
        
        if (!orderBelongsToCustomer) {
            return ResponseUtil.createErrorResponse(
                    "FORBIDDEN",
                    "Không có quyền truy cập",
                    "Biên bản này không thuộc về đơn hàng của bạn",
                    HttpStatus.FORBIDDEN
            );
        }
        
        return ResponseUtil.createSuccessResponse(
                "Lấy biên bản bàn giao thành công",
                "Chi tiết biên bản bàn giao",
                report,
                HttpStatus.OK
        );
    }

    @PostMapping("/{handoverReportId}/pin")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> requestPinForCustomer(
            @PathVariable Long handoverReportId,
            @Valid @RequestBody EmailPinRequestDto request,
            @AuthenticationPrincipal UserDetails principal) {
        getCustomerFromPrincipal(principal); // Verify customer exists
        HandoverReportResponseDto report = handoverReportService.getReport(handoverReportId);
        
        // Verify the report belongs to customer's order
        if (report.getOrderId() == null) {
            return ResponseUtil.createErrorResponse(
                    "INVALID_REPORT",
                    "Biên bản không hợp lệ",
                    "Biên bản không có thông tin đơn hàng",
                    HttpStatus.BAD_REQUEST
            );
        }

        HandoverPinDeliveryDto responseDto = handoverReportService.sendPinToCustomerForReport(handoverReportId, request.getEmail());
        return ResponseUtil.createSuccessResponse(
                "Gửi PIN thành công",
                "Mã PIN đã được gửi đến email của bạn",
                responseDto,
                HttpStatus.OK
        );
    }

    @PatchMapping("/{handoverReportId}/signature")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> signByCustomer(
            @PathVariable Long handoverReportId,
            @Valid @RequestBody HandoverReportCustomerSignRequestDto request,
            @AuthenticationPrincipal UserDetails principal) {
        getCustomerFromPrincipal(principal); // Verify customer exists
        HandoverReportResponseDto report = handoverReportService.getReport(handoverReportId);
        
        // Verify the report belongs to customer's order
        if (report.getOrderId() == null) {
            return ResponseUtil.createErrorResponse(
                    "INVALID_REPORT",
                    "Biên bản không hợp lệ",
                    "Biên bản không có thông tin đơn hàng",
                    HttpStatus.BAD_REQUEST
            );
        }

        HandoverReportResponseDto responseDto = handoverReportService.signByCustomer(handoverReportId, request);
        return ResponseUtil.createSuccessResponse(
                "Ký biên bản thành công",
                "Bạn đã ký biên bản bàn giao. Đơn hàng đã được kích hoạt.",
                responseDto,
                HttpStatus.OK
        );
    }

    private Customer getCustomerFromPrincipal(UserDetails principal) {
        try {
            String username = principal.getUsername();
            return customerService.getCustomerByUsernameOrThrow(username);
        } catch (Exception e) {
            throw new RuntimeException("Không thể lấy thông tin customer: " + e.getMessage());
        }
    }
}

