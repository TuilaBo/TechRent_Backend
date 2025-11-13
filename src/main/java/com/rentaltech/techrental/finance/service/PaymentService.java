package com.rentaltech.techrental.finance.service;

import com.rentaltech.techrental.finance.model.dto.CreatePaymentRequest;
import com.rentaltech.techrental.finance.model.dto.CreatePaymentResponse;
import com.rentaltech.techrental.finance.model.dto.InvoiceResponseDto;
import com.rentaltech.techrental.finance.model.dto.TransactionResponseDto;
import vn.payos.model.webhooks.WebhookData;

import java.util.List;

public interface PaymentService {

    CreatePaymentResponse createPayment(CreatePaymentRequest request);

    void handleWebhook(WebhookData webhookData);

    InvoiceResponseDto getInvoiceForCustomer(Long rentalOrderId, String username);

    List<TransactionResponseDto> getAllTransactions();
}
