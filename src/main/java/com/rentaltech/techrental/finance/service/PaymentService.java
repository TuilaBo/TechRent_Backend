package com.rentaltech.techrental.finance.service;

import com.rentaltech.techrental.finance.model.dto.CreatePaymentRequest;
import com.rentaltech.techrental.finance.model.dto.CreatePaymentResponse;
import com.rentaltech.techrental.finance.model.dto.InvoiceResponseDto;
import com.rentaltech.techrental.finance.model.dto.TransactionResponseDto;
import vn.payos.model.webhooks.WebhookData;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PaymentService {

    CreatePaymentResponse createPayment(CreatePaymentRequest request);

    void handleWebhook(WebhookData webhookData);

    boolean handlePayOsReturn(Long orderCode, boolean success);

    List<InvoiceResponseDto> getInvoiceForCustomer(Long rentalOrderId, String username);

    List<TransactionResponseDto> getAllTransactions();
    List<InvoiceResponseDto> getAllInvoices();

    InvoiceResponseDto confirmDepositRefund(Long settlementId, String username, MultipartFile proofFile);
}
