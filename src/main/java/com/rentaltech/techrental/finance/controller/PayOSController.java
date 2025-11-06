package com.rentaltech.techrental.finance.controller;

import com.rentaltech.techrental.finance.service.PaymentService;
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
public class PayOSController {

    private final PaymentService paymentService;
    private final PayOS payOS;

    @PostMapping
    public ResponseEntity<?> handleWebhook(@RequestBody String payload) {
        WebhookData data = payOS.webhooks().verify(payload);
        paymentService.handleWebhook(data);
        return ResponseEntity.ok().build();
    }
}
