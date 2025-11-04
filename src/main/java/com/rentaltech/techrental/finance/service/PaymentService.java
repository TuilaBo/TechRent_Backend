package com.rentaltech.techrental.finance.service;

import com.rentaltech.techrental.finance.model.dto.CreatePaymentRequest;
import com.rentaltech.techrental.finance.model.dto.CreatePaymentResponse;
import com.rentaltech.techrental.finance.model.dto.InvoiceResponseDto;
import vn.payos.model.webhooks.WebhookData;

public interface PaymentService {

    CreatePaymentResponse createPayment(CreatePaymentRequest request);

    void handleWebhook(WebhookData webhookData);

    InvoiceResponseDto getInvoiceForCustomer(Long rentalOrderId, String username);
}
