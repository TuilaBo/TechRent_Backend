package com.rentaltech.techrental.finance.service;

import com.rentaltech.techrental.finance.config.VnpayConfig;
import com.rentaltech.techrental.finance.model.*;
import com.rentaltech.techrental.finance.model.dto.CreatePaymentRequest;
import com.rentaltech.techrental.finance.model.dto.CreatePaymentResponse;
import com.rentaltech.techrental.finance.model.dto.InvoiceResponseDto;
import com.rentaltech.techrental.finance.repository.InvoiceRepository;
import com.rentaltech.techrental.finance.repository.TransactionRepository;
import com.rentaltech.techrental.finance.util.VnpayUtil;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.rentalorder.service.ReservationService;
import com.rentaltech.techrental.staff.model.Task;
import com.rentaltech.techrental.staff.model.TaskCategory;
import com.rentaltech.techrental.staff.model.TaskStatus;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String DELIVERY_CATEGORY_NAME = "DELIVERY";

    private final PayOS payOS;
    private final VnpayConfig vnpayConfig;
    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final RentalOrderRepository rentalOrderRepository;
    private final NotificationService notificationService;
    private final TaskRepository taskRepository;
    private final TaskCategoryRepository taskCategoryRepository;

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

        if (paymentMethod != PaymentMethod.PAYOS && paymentMethod != PaymentMethod.VNPAY) {
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
        
        Invoice invoice;
        if (paymentMethod == PaymentMethod.VNPAY) {
            String vnpayTransactionId = generateVnpayTransactionId();
            invoice = Invoice.builder()
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
                    .vnpayTransactionId(vnpayTransactionId)
                    .frontendSuccessUrl(request.getFrontendSuccessUrl())
                    .frontendFailureUrl(request.getFrontendFailureUrl())
                    .build();
            invoice = invoiceRepository.save(invoice);
            
            String paymentUrl = buildVnpayPaymentUrl(request, invoice);
            return CreatePaymentResponse.builder()
                    .checkoutUrl(paymentUrl)
                    .orderCode(Long.parseLong(vnpayTransactionId))
                    .status("PENDING")
                    .build();
        } else {
            long payosOrderCode = generateUniquePayosOrderCode();
            invoice = Invoice.builder()
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
                    .payosOrderCode(payosOrderCode)
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

        if (orderCode != null && orderCode == 123L) {
            log.info("PayOS connectivity ping received, skipping business processing.");
            return;
        }

        Optional<InvoiceStatus> targetStatus = code.equals("00") ? Optional.of(InvoiceStatus.SUCCEEDED) : Optional.of(InvoiceStatus.FAILED);

        InvoiceStatus newStatus = targetStatus.get();

        Invoice invoice = invoiceRepository.findByPayosOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found for PayOS order code: " + orderCode));
        RentalOrder rentalOrder = invoice.getRentalOrder();

        invoice.setInvoiceStatus(newStatus);

        if (newStatus == InvoiceStatus.SUCCEEDED) {
            invoice.setPaymentDate(LocalDateTime.now());

            if (rentalOrder != null) {
                rentalOrder.setOrderStatus(OrderStatus.DELIVERY_CONFIRMED);
                rentalOrderRepository.save(rentalOrder);
            }

            boolean transactionCreated = false;
            if (!transactionRepository.existsByInvoice(invoice)) {
                BigDecimal transactionAmount = Optional.ofNullable(invoice.getTotalAmount()).orElse(BigDecimal.ZERO);

                Transaction transaction = Transaction.builder()
                        .amount(transactionAmount)
                        .transactionType(TrasactionType.TRANSACTION_IN)
                        .invoice(invoice)
                        .build();

                transactionRepository.save(transaction);
                transactionCreated = true;
            }

            if (transactionCreated && rentalOrder != null) {
                notifyPaymentSuccess(rentalOrder);
                createDeliveryTaskIfNeeded(rentalOrder);
            }

        } else {
            invoice.setPaymentDate(null);
        }

        invoiceRepository.save(invoice);
    }


    private void notifyPaymentSuccess(RentalOrder rentalOrder) {
        if (rentalOrder.getCustomer() == null || rentalOrder.getCustomer().getCustomerId() == null) {
            return;
        }
        String title = "Thanh toán thành công";
        String message = String.format("Đơn thuê #%d đã được xác nhận thanh toán.", rentalOrder.getOrderId());
        notificationService.notifyCustomer(
                rentalOrder.getCustomer().getCustomerId(),
                NotificationType.ORDER_CONFIRMED,
                title,
                message
        );
    }

    private void createDeliveryTaskIfNeeded(RentalOrder rentalOrder) {
        if (rentalOrder.getOrderId() == null) {
            return;
        }
        TaskCategory deliveryCategory = taskCategoryRepository.findByNameIgnoreCase(DELIVERY_CATEGORY_NAME)
                .orElseGet(() -> taskCategoryRepository.findByName(DELIVERY_CATEGORY_NAME).orElse(null));
        if (deliveryCategory == null) {
            log.warn("Không thể tạo task giao hàng vì không tìm thấy category '{}'", DELIVERY_CATEGORY_NAME);
            return;
        }
        boolean alreadyExists = taskRepository.findByOrderId(rentalOrder.getOrderId()).stream()
                .anyMatch(task -> task.getTaskCategory() != null
                        && task.getTaskCategory().getTaskCategoryId().equals(deliveryCategory.getTaskCategoryId()));
        if (alreadyExists) {
            return;
        }
        Task deliveryTask = Task.builder()
                .taskCategory(deliveryCategory)
                .orderId(rentalOrder.getOrderId())
                .type(DELIVERY_CATEGORY_NAME)
                .description(String.format("Chuẩn bị giao đơn hàng #%d cho khách.", rentalOrder.getOrderId()))
                .plannedStart(LocalDateTime.now())
                .status(TaskStatus.PENDING)
                .build();
        taskRepository.save(deliveryTask);
    }

    private CreatePaymentLinkRequest buildCreatePaymentRequest(CreatePaymentRequest request, Invoice invoice) {
        if (request.getReturnUrl() == null) {
            throw new IllegalStateException("Return URL not configured for PayOS payment");
        }
        if (request.getCancelUrl() == null) {
            throw new IllegalStateException("Cancel URL not configured for PayOS payment");
        }

        PaymentLinkItem itemData = PaymentLinkItem.builder()
                .name("Đơn hàng TechRent: " + invoice.getRentalOrder().getOrderId())
                .quantity(1)
                .price(invoice.getTotalAmount().longValueExact())
                .build();

        return CreatePaymentLinkRequest.builder()
                        .orderCode(invoice.getPayosOrderCode())
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

    private static final long PAYOS_SAFE_MAX = 9_007_199_254_740_991L;

    private long generateUniquePayosOrderCode() {
        long code;
        do {
            code = ThreadLocalRandom.current().nextLong(1, PAYOS_SAFE_MAX);
        } while (invoiceRepository.existsByPayosOrderCode(code));
        return code;
    }

    private String generateVnpayTransactionId() {
        String transactionId;
        do {
            transactionId = String.valueOf(System.currentTimeMillis());
        } while (invoiceRepository.findByVnpayTransactionId(transactionId).isPresent());
        return transactionId;
    }

    private String buildVnpayPaymentUrl(CreatePaymentRequest request, Invoice invoice) {
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(invoice.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue()));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", invoice.getVnpayTransactionId());
        // Use order info without spaces to avoid hash issues
        vnpParams.put("vnp_OrderInfo", "Thanhtoandonhang" + invoice.getRentalOrder().getOrderId());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        
        // Use returnUrl from request if provided and valid, otherwise use IPN URL as fallback
        // (VNPAY requires return URL, but we'll use IPN URL since both handle callbacks the same way)
        String returnUrl = (request.getReturnUrl() != null 
                && !request.getReturnUrl().trim().isEmpty() 
                && !request.getReturnUrl().equals("string"))
                ? request.getReturnUrl().trim()
                : vnpayConfig.getIpnUrl(); // Use IPN URL as return URL fallback
        vnpParams.put("vnp_ReturnUrl", returnUrl);
        // VNPAY may require actual IP, but for sandbox 127.0.0.1 should work
        // In production, get real client IP from request
        vnpParams.put("vnp_IpAddr", "127.0.0.1");
        vnpParams.put("vnp_CreateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        
        // Create hash BEFORE adding vnp_SecureHash to params
        String secureHash = VnpayUtil.hashAllFields(vnpParams, vnpayConfig.getHashSecret());
        if (secureHash == null) {
            throw new IllegalStateException("Failed to generate VNPAY secure hash");
        }
        
        log.info("VNPAY params before hash: {}", vnpParams);
        log.info("VNPAY hash secret: {}", vnpayConfig.getHashSecret());
        log.info("VNPAY secure hash: {}", secureHash);
        
        // Add hash to params AFTER hashing
        vnpParams.put("vnp_SecureHash", secureHash);
        
        return VnpayUtil.getPaymentUrl(vnpParams, vnpayConfig.getUrl());
    }

    @Transactional
    public void handleVnpayCallback(Map<String, String> params) {
        String vnp_SecureHash = params.get("vnp_SecureHash");
        String vnp_TxnRef = params.get("vnp_TxnRef");
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String vnp_TransactionStatus = params.get("vnp_TransactionStatus");
        
        log.info("VNPAY callback params received: {}", params);
        log.info("VNPAY vnp_SecureHash from request: {}", vnp_SecureHash);
        
        Map<String, String> paramsForHash = new HashMap<>(params);
        String secureHash = VnpayUtil.hashAllFields(paramsForHash, vnpayConfig.getHashSecret());
        
        log.info("VNPAY calculated hash: {}", secureHash);
        log.info("VNPAY hash secret used: {}", vnpayConfig.getHashSecret());
        
        if (!secureHash.equals(vnp_SecureHash)) {
            log.error("Invalid VNPAY checksum for transaction: {}. Expected: {}, Got: {}", vnp_TxnRef, secureHash, vnp_SecureHash);
            log.error("All params for hash: {}", paramsForHash);
            throw new IllegalStateException("Invalid VNPAY checksum");
        }
        
        Invoice invoice = invoiceRepository.findByVnpayTransactionId(vnp_TxnRef)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found for VNPAY transaction: " + vnp_TxnRef));
        
        boolean isSuccess = "00".equals(vnp_ResponseCode) && "00".equals(vnp_TransactionStatus);
        InvoiceStatus newStatus = isSuccess ? InvoiceStatus.SUCCEEDED : InvoiceStatus.FAILED;
        
        invoice.setInvoiceStatus(newStatus);
        
        if (newStatus == InvoiceStatus.SUCCEEDED) {
            invoice.setPaymentDate(LocalDateTime.now());
            
            RentalOrder rentalOrder = invoice.getRentalOrder();
            if (rentalOrder != null) {
                rentalOrder.setOrderStatus(OrderStatus.DELIVERY_CONFIRMED);
                rentalOrderRepository.save(rentalOrder);
            }
            
            boolean transactionCreated = false;
            if (!transactionRepository.existsByInvoice(invoice)) {
                BigDecimal transactionAmount = Optional.ofNullable(invoice.getTotalAmount()).orElse(BigDecimal.ZERO);
                
                Transaction transaction = Transaction.builder()
                        .amount(transactionAmount)
                        .transactionType(TrasactionType.TRANSACTION_IN)
                        .invoice(invoice)
                        .build();
                
                transactionRepository.save(transaction);
                transactionCreated = true;
            }
            
            if (transactionCreated && rentalOrder != null) {
                notifyPaymentSuccess(rentalOrder);
                createDeliveryTaskIfNeeded(rentalOrder);
            }
        } else {
            invoice.setPaymentDate(null);
        }
        
        invoiceRepository.save(invoice);
    }
}
