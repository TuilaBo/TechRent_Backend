package com.rentaltech.techrental.finance.controller;

import com.rentaltech.techrental.finance.config.VnpayConfig;
import com.rentaltech.techrental.finance.model.Invoice;
import com.rentaltech.techrental.finance.repository.InvoiceRepository;
import com.rentaltech.techrental.finance.service.PaymentServiceImpl;
import com.rentaltech.techrental.finance.util.VnpayUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/vnpay")
@Slf4j
public class VnpayController {

    private final PaymentServiceImpl paymentService;
    private final VnpayConfig vnpayConfig;
    private final InvoiceRepository invoiceRepository;

    @GetMapping("/return")
    public void returnUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, String> params = VnpayUtil.getRequestParams(request);
            log.info("VNPAY return URL called with params: {}", params);
            paymentService.handleVnpayCallback(params);
            
            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            String vnp_TxnRef = params.get("vnp_TxnRef");
            
            // Get frontend URLs from Invoice if available, otherwise use config
            String frontendSuccessUrl = vnpayConfig.getFrontendSuccessUrl();
            String frontendFailureUrl = vnpayConfig.getFrontendFailureUrl();
            
            if (vnp_TxnRef != null) {
                Invoice invoice = invoiceRepository.findByVnpayTransactionId(vnp_TxnRef).orElse(null);
                if (invoice != null) {
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
                String redirectUrl = frontendSuccessUrl 
                        + "?transactionId=" + URLEncoder.encode(vnp_TxnRef != null ? vnp_TxnRef : "", StandardCharsets.UTF_8)
                        + "&status=success";
                log.info("Redirecting to success page: {}", redirectUrl);
                response.sendRedirect(redirectUrl);
            } else {
                // Payment failed - redirect to failure page
                String redirectUrl = frontendFailureUrl 
                        + "?transactionId=" + URLEncoder.encode(vnp_TxnRef != null ? vnp_TxnRef : "", StandardCharsets.UTF_8)
                        + "&status=failed"
                        + "&code=" + URLEncoder.encode(vnp_ResponseCode != null ? vnp_ResponseCode : "", StandardCharsets.UTF_8);
                log.info("Redirecting to failure page: {}", redirectUrl);
                response.sendRedirect(redirectUrl);
            }
        } catch (Exception e) {
            log.error("Error processing VNPAY return URL", e);
            // Redirect to failure page on error
            String redirectUrl = vnpayConfig.getFrontendFailureUrl() 
                    + "?error=" + URLEncoder.encode(e.getMessage() != null ? e.getMessage() : "Unknown error", StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
        }
    }

    @RequestMapping(value = "/ipn", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> ipnUrl(HttpServletRequest request) {
        try {
            Map<String, String> params = VnpayUtil.getRequestParams(request);
            log.info("VNPAY IPN called with params: {}", params);
            paymentService.handleVnpayCallback(params);
            return ResponseEntity.ok().body("OK");
        } catch (Exception e) {
            log.error("Error processing VNPAY IPN", e);
            return ResponseEntity.badRequest().body("Error processing IPN: " + e.getMessage());
        }
    }

    @GetMapping("/test-hash")
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

