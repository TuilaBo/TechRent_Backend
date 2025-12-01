package com.rentaltech.techrental.finance.controller;

import com.rentaltech.techrental.finance.model.Invoice;
import com.rentaltech.techrental.finance.repository.InvoiceRepository;
import com.rentaltech.techrental.finance.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payos")
@Tag(name = "PayOS Webhook", description = "Webhook xử lý thông báo thanh toán từ PayOS")
@Slf4j
public class PayOSController {

    private final PaymentService paymentService;
    private final InvoiceRepository invoiceRepository;

    @Value("${payos.frontend-success-url:http://localhost:5173/success}")
    private String defaultFrontendSuccessUrl;

    @Value("${payos.frontend-failure-url:http://localhost:5173/failure}")
    private String defaultFrontendFailureUrl;

    @GetMapping("/return")
    @Operation(summary = "URL trả về PayOS", description = "Trình duyệt được chuyển hướng về điểm cuối này khi thanh toán thành công")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Chuyển sang trang thành công của frontend"),
            @ApiResponse(responseCode = "500", description = "Không chuyển hướng được do lỗi nội bộ")
    })
    public void handleReturn(@RequestParam(name = "orderCode", required = false) Long orderCode,
                             @RequestParam(name = "status", required = false) String status,
                             HttpServletRequest request,
                             HttpServletResponse response) throws IOException {
        log.info("=== PayOS RETURN URL CALLED ===");
        log.info("Request method: {}", request.getMethod());
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Query string: {}", request.getQueryString());
        log.info("Remote address: {}", request.getRemoteAddr());
        boolean requestedSuccess = "PAID".equalsIgnoreCase(status);
        boolean success = paymentService.handlePayOsReturn(orderCode, requestedSuccess);
        redirectToFrontend(response, orderCode, success);
    }

    @GetMapping("/cancel")
    @Operation(summary = "URL hủy PayOS", description = "Trình duyệt được chuyển hướng về điểm cuối này khi thanh toán thất bại hoặc bị hủy")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Chuyển sang trang thất bại của frontend"),
            @ApiResponse(responseCode = "500", description = "Không chuyển hướng được do lỗi nội bộ")
    })
    public void handleCancel(@RequestParam(name = "orderCode", required = false) Long orderCode,
                             @RequestParam(name = "status", required = false) String status,
                             @RequestParam(name = "message", required = false) String message,
                             HttpServletRequest request,
                             HttpServletResponse response) throws IOException {
        log.info("=== PayOS CANCEL URL CALLED ===");
        log.info("Request method: {}", request.getMethod());
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Query string: {}", request.getQueryString());
        log.info("Remote address: {}", request.getRemoteAddr());
        paymentService.handlePayOsReturn(orderCode, false);
        redirectToFrontend(response, orderCode, false);
    }

    private void redirectToFrontend(HttpServletResponse response,
                                    Long orderCode,
                                    boolean success) throws IOException {
        String targetUrl = success ? defaultFrontendSuccessUrl : defaultFrontendFailureUrl;
        String statusParam = success ? "success" : "failed";
        Long rentalOrderId = null;

        if (orderCode != null) {
            Invoice invoice = invoiceRepository.findByPayosOrderCode(orderCode).orElse(null);
            if (invoice != null) {
                rentalOrderId = invoice.getRentalOrder() != null ? invoice.getRentalOrder().getOrderId() : null;
                if (success && StringUtils.hasText(invoice.getFrontendSuccessUrl())) {
                    targetUrl = invoice.getFrontendSuccessUrl();
                } else if (!success && StringUtils.hasText(invoice.getFrontendFailureUrl())) {
                    targetUrl = invoice.getFrontendFailureUrl();
                }
            }
        }

        StringBuilder redirectUrl = new StringBuilder(targetUrl);
        String separator = targetUrl.contains("?") ? "&" : "?";
        redirectUrl.append(separator)
                .append("status=").append(URLEncoder.encode(statusParam, StandardCharsets.UTF_8));
        if (orderCode != null) {
            redirectUrl.append("&orderCode=").append(URLEncoder.encode(String.valueOf(orderCode), StandardCharsets.UTF_8));
        }
        if (rentalOrderId != null) {
            redirectUrl.append("&orderId=").append(URLEncoder.encode(String.valueOf(rentalOrderId), StandardCharsets.UTF_8));
        }

        response.sendRedirect(redirectUrl.toString());
    }
}
