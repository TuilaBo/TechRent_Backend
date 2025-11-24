package com.rentaltech.techrental.finance.controller;

import com.rentaltech.techrental.finance.config.VnpayConfig;
import com.rentaltech.techrental.finance.model.Invoice;
import com.rentaltech.techrental.finance.repository.InvoiceRepository;
import com.rentaltech.techrental.finance.service.PaymentServiceImpl;
import com.rentaltech.techrental.finance.util.VnpayUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/vnpay")
@Slf4j
@Tag(name = "VNPAY", description = "Các webhook/endpoint tích hợp thanh toán VNPAY")
public class VnpayController {

    private final PaymentServiceImpl paymentService;
    private final VnpayConfig vnpayConfig;
    private final InvoiceRepository invoiceRepository;

    @GetMapping("/return")
    @Operation(summary = "VNPAY return URL", description = "VNPAY gọi lại khi người dùng hoàn tất thanh toán trên cổng")
    public void returnUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("=== VNPAY RETURN URL CALLED ===");
        log.info("Request method: {}", request.getMethod());
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Query string: {}", request.getQueryString());
        log.info("Remote address: {}", request.getRemoteAddr());
        try {
            Map<String, String> params = VnpayUtil.getRequestParams(request);
            log.info("VNPAY return URL called with params: {}", params);
            log.info("Calling handleVnpayCallback...");
            paymentService.handleVnpayCallback(params);
            log.info("handleVnpayCallback completed successfully");
            
            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            String vnp_TxnRef = params.get("vnp_TxnRef");
            
            // Get frontend URLs from Invoice if available, otherwise use config
            String frontendSuccessUrl = vnpayConfig.getFrontendSuccessUrl();
            String frontendFailureUrl = vnpayConfig.getFrontendFailureUrl();
            Long orderId = null;
            
            if (vnp_TxnRef != null) {
                Invoice invoice = invoiceRepository.findByVnpayTransactionId(vnp_TxnRef).orElse(null);
                if (invoice != null) {
                    orderId = invoice.getRentalOrder() != null ? invoice.getRentalOrder().getOrderId() : null;
                    if (StringUtils.hasText(invoice.getFrontendSuccessUrl())) {
                        frontendSuccessUrl = invoice.getFrontendSuccessUrl();
                    }
                    if (StringUtils.hasText(invoice.getFrontendFailureUrl())) {
                        frontendFailureUrl = invoice.getFrontendFailureUrl();
                    }
                }
            }
            
            // Redirect to frontend based on payment result
            if ("00".equals(vnp_ResponseCode)) {
                // Payment successful - redirect to success page
                StringBuilder redirectUrl = new StringBuilder(frontendSuccessUrl);
                String separator = frontendSuccessUrl.contains("?") ? "&" : "?";
                redirectUrl.append(separator).append("transactionId=").append(URLEncoder.encode(vnp_TxnRef != null ? vnp_TxnRef : "", StandardCharsets.UTF_8));
                redirectUrl.append("&status=success");
                if (orderId != null) {
                    redirectUrl.append("&orderId=").append(URLEncoder.encode(String.valueOf(orderId), StandardCharsets.UTF_8));
                }
                log.info("Redirecting to success page: {}", redirectUrl);
                response.sendRedirect(redirectUrl.toString());
            } else {
                // Payment failed - redirect to failure page
                StringBuilder redirectUrl = new StringBuilder(frontendFailureUrl);
                String separator = frontendFailureUrl.contains("?") ? "&" : "?";
                redirectUrl.append(separator).append("transactionId=").append(URLEncoder.encode(vnp_TxnRef != null ? vnp_TxnRef : "", StandardCharsets.UTF_8));
                redirectUrl.append("&status=failed");
                redirectUrl.append("&code=").append(URLEncoder.encode(vnp_ResponseCode != null ? vnp_ResponseCode : "", StandardCharsets.UTF_8));
                if (orderId != null) {
                    redirectUrl.append("&orderId=").append(URLEncoder.encode(String.valueOf(orderId), StandardCharsets.UTF_8));
                }
                log.info("Redirecting to failure page: {}", redirectUrl);
                response.sendRedirect(redirectUrl.toString());
            }
        } catch (Exception e) {
            log.error("Error processing VNPAY return URL", e);
            // Redirect to failure page on error
            String frontendFailureUrl = vnpayConfig.getFrontendFailureUrl();
            StringBuilder redirectUrl = new StringBuilder(frontendFailureUrl);
            String separator = frontendFailureUrl.contains("?") ? "&" : "?";
            redirectUrl.append(separator).append("error=").append(URLEncoder.encode(e.getMessage() != null ? e.getMessage() : "Unknown error", StandardCharsets.UTF_8));
            response.sendRedirect(redirectUrl.toString());
        }
    }

    @RequestMapping(value = "/ipn", method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "VNPAY IPN", description = "Điểm nhận thông báo thanh toán từ máy chủ VNPAY")
    public ResponseEntity<?> ipnUrl(HttpServletRequest request) {
        log.info("=== VNPAY IPN URL CALLED ===");
        log.info("Request method: {}", request.getMethod());
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Query string: {}", request.getQueryString());
        log.info("Content type: {}", request.getContentType());
        log.info("Remote address: {}", request.getRemoteAddr());
        try {
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                java.util.List<String> headers = java.util.Collections.list(headerNames);
                log.info("Headers: {}", headers);
            }
        } catch (Exception e) {
            log.debug("Could not log headers: {}", e.getMessage());
        }
        try {
            Map<String, String> params = VnpayUtil.getRequestParams(request);
            log.info("VNPAY IPN called with params: {}", params);
            
            if (params.isEmpty()) {
                log.warn("VNPAY IPN received empty params - this might be a test ping or invalid request");
                return ResponseEntity.ok().body("OK");
            }
            
            log.info("Calling handleVnpayCallback from IPN...");
            paymentService.handleVnpayCallback(params);
            log.info("handleVnpayCallback from IPN completed successfully");
            return ResponseEntity.ok().body("OK");
        } catch (Exception e) {
            log.error("Error processing VNPAY IPN", e);
            log.error("Exception stack trace: ", e);
            return ResponseEntity.badRequest().body("Error processing IPN: " + e.getMessage());
        }
    }

    @GetMapping("/test-hash")
    @Operation(summary = "Kiểm thử hash", description = "Endpoint hỗ trợ debug chữ ký trả về từ VNPAY")
    public ResponseEntity<?> testHash(HttpServletRequest request) {
        try {
            Map<String, String> params = VnpayUtil.getRequestParams(request);
            log.info("Test hash with params: {}", params);
            
            // Calculate hash
            String calculatedHash = VnpayUtil.hashAllFields(params, "7P3XI2WYW9U345M89WWNV2EWHCED4WXJ");
            String receivedHash = params.get("vnp_SecureHash");
            
            Map<String, Object> result = new HashMap<>();
            result.put("receivedHash", receivedHash);
            result.put("calculatedHash", calculatedHash);
            result.put("match", calculatedHash != null && calculatedHash.equals(receivedHash));
            result.put("params", params);
            
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            log.error("Error testing hash", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}

