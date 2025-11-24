package com.rentaltech.techrental.finance.controller;

import com.rentaltech.techrental.finance.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhook")
@Tag(name = "PayOS Webhook", description = "Webhook xử lý thông báo thanh toán từ PayOS")
public class PayOSController {

    private final PaymentService paymentService;
    private final PayOS payOS;

    @PostMapping
    @Operation(summary = "PayOS webhook", description = "PayOS gửi sự kiện thanh toán đến endpoint này")
    public ResponseEntity<?> handleWebhook(@RequestBody String payload) {
        WebhookData data = payOS.webhooks().verify(payload);
        paymentService.handleWebhook(data);
        return ResponseEntity.ok().build();
    }
}
