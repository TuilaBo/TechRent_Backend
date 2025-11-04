package com.rentaltech.techrental.finance.service;

import com.rentaltech.techrental.finance.model.*;
import com.rentaltech.techrental.payment.model.*;
import com.rentaltech.techrental.finance.model.dto.CreatePaymentRequest;
import com.rentaltech.techrental.finance.model.dto.CreatePaymentResponse;
import com.rentaltech.techrental.finance.model.dto.InvoiceResponseDto;
import com.rentaltech.techrental.finance.repository.InvoiceRepository;
import com.rentaltech.techrental.finance.repository.TransactionRepository;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PayOS payOS;
    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final RentalOrderRepository rentalOrderRepository;

    @Override
    @Transactional
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        InvoiceType invoiceType = request.getInvoiceType();
        if (invoiceType != InvoiceType.RENT_PAYMENT) {
            throw new UnsupportedOperationException("TODO: unsupported invoice type " + invoiceType);
        }

        PaymentMethod paymentMethod = request.getPaymentMethod();
        if (paymentMethod == PaymentMethod.MOMO || paymentMethod == PaymentMethod.BANK_ACCOUNT) {
            throw new UnsupportedOperationException("TODO: unsupported payment method " + paymentMethod);
        }

        if (paymentMethod != PaymentMethod.PAYOS) {
            throw new IllegalArgumentException("Unsupported payment method: " + paymentMethod);
        }

        RentalOrder rentalOrder = rentalOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Rental order not found: " + request.getOrderId()));

        BigDecimal expectedSubTotal = safeSum(rentalOrder.getDepositAmount(), rentalOrder.getTotalPrice());
        if (request.getAmount() == null || request.getAmount().compareTo(expectedSubTotal) != 0) {
            throw new IllegalArgumentException("Amount does not match deposit + total price for order " + request.getOrderId());
        }

        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal depositApplied = BigDecimal.ZERO;
        BigDecimal totalAmount = expectedSubTotal.add(taxAmount).subtract(discountAmount);
        Invoice invoice = Invoice.builder()
                .rentalOrder(rentalOrder)
                .invoiceType(invoiceType)
                .paymentMethod(paymentMethod)
                .paymentDate(null)
                .subTotal(expectedSubTotal)
                .taxAmount(taxAmount)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .depositApplied(depositApplied)
                .dueDate(LocalDateTime.now().plusDays(3))
                .invoiceStatus(InvoiceStatus.PROCESSING)
                .pdfUrl(null)
                .issueDate(null)
                .build();

        invoice = invoiceRepository.save(invoice);

        CreatePaymentLinkRequest paymentLinkRequest = buildCreatePaymentRequest(request, invoice);
        try {
            CreatePaymentLinkResponse response = payOS.paymentRequests().create(paymentLinkRequest);

            return CreatePaymentResponse.builder()
                    .paymentLinkId(response.getPaymentLinkId())
                    .checkoutUrl(response.getCheckoutUrl())
                    .qrCodeUrl(response.getQrCode())
                    .orderCode(response.getOrderCode())
                    .status(response.getStatus().getValue())
                    .build();
        } catch (PayOSException ex) {
            throw new IllegalStateException("Failed to create PayOS payment link", ex);
        }
    }

    @Override
    @Transactional
    public void handleWebhook(WebhookData data) {

        // ✅ Trích dữ liệu cần
        Long orderCode = data.getOrderCode();
        String code = data.getCode();
        String eventDesc = data.getDesc();
        Long amount = data.getAmount();

        log.info("Received PayOS webhook: orderCode={}, desc={}, amount={}, code={}",
                orderCode, eventDesc, amount, code);

        Optional<InvoiceStatus> targetStatus = code.equals("00") ? Optional.of(InvoiceStatus.SUCCEEDED) : Optional.of(InvoiceStatus.FAILED);

        InvoiceStatus newStatus = targetStatus.get();

        Invoice invoice = invoiceRepository.findById(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + orderCode));

        invoice.setInvoiceStatus(newStatus);

        if (newStatus == InvoiceStatus.SUCCEEDED) {
            invoice.setPaymentDate(LocalDateTime.now());

            RentalOrder rentalOrder = invoice.getRentalOrder();
            if (rentalOrder != null) {
                rentalOrder.setOrderStatus(OrderStatus.DELIVERY_CONFIRMED);
                rentalOrderRepository.save(rentalOrder);
            }

            if (!transactionRepository.existsByInvoice(invoice)) {
                BigDecimal transactionAmount = Optional.ofNullable(invoice.getTotalAmount()).orElse(BigDecimal.ZERO);

                Transaction transaction = Transaction.builder()
                        .amount(transactionAmount)
                        .transactionType(TrasactionType.TRANSACTION_IN)
                        .invoice(invoice)
                        .build();

                transactionRepository.save(transaction);
            }

        } else {
            invoice.setPaymentDate(null);
        }

        invoiceRepository.save(invoice);
    }


    private CreatePaymentLinkRequest buildCreatePaymentRequest(CreatePaymentRequest request, Invoice invoice) {
        if (request.getReturnUrl() == null) {
            throw new IllegalStateException("Return URL not configured for PayOS payment");
        }
        if (request.getCancelUrl() == null) {
            throw new IllegalStateException("Cancel URL not configured for PayOS payment");
        }

        PaymentLinkItem itemData = PaymentLinkItem.builder()
                .name("Mỳ tôm Hảo Hảo ly")
                .quantity(1)
                .price(2000L)
                .build();

        return CreatePaymentLinkRequest.builder()
                        .orderCode(invoice.getInvoiceId())
                        .amount(request.getAmount().longValueExact())
                        .description("Thanh toan")
                        .returnUrl("https://your-url.com/success")
                        .cancelUrl("https://your-url.com/cancel")
                        .item(itemData)
                        .build();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponseDto getInvoiceForCustomer(Long rentalOrderId, String username) {
        Invoice invoice = invoiceRepository.findFirstByRentalOrder_OrderIdOrderByInvoiceIdDesc(rentalOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn cho đơn hàng: " + rentalOrderId));

        RentalOrder rentalOrder = invoice.getRentalOrder();

        String ownerUsername = rentalOrder.getCustomer().getAccount().getUsername();
        if (ownerUsername == null || !ownerUsername.equalsIgnoreCase(username)) {
            throw new AccessDeniedException("Bạn không thể xem hóa đơn của đơn hàng này");
        }

        return InvoiceResponseDto.from(invoice);
    }

    private BigDecimal safeSum(BigDecimal first, BigDecimal second) {
        BigDecimal left = Optional.ofNullable(first).orElse(BigDecimal.ZERO);
        BigDecimal right = Optional.ofNullable(second).orElse(BigDecimal.ZERO);
        return left.add(right);
    }
}
