package com.rentaltech.techrental.finance.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.repository.AccountRepository;
import com.rentaltech.techrental.contract.model.Contract;
import com.rentaltech.techrental.contract.model.ContractStatus;
import com.rentaltech.techrental.contract.repository.ContractExtensionAnnexRepository;
import com.rentaltech.techrental.contract.repository.ContractRepository;
import com.rentaltech.techrental.finance.config.VnpayConfig;
import com.rentaltech.techrental.finance.model.*;
import com.rentaltech.techrental.finance.model.dto.CreatePaymentRequest;
import com.rentaltech.techrental.finance.model.dto.CreatePaymentResponse;
import com.rentaltech.techrental.finance.model.dto.InvoiceResponseDto;
import com.rentaltech.techrental.finance.model.dto.TransactionResponseDto;
import com.rentaltech.techrental.finance.repository.InvoiceRepository;
import com.rentaltech.techrental.finance.repository.TransactionRepository;
import com.rentaltech.techrental.finance.util.VnpayUtil;
import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.repository.AllocationRepository;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.rentalorder.service.BookingCalendarService;
import com.rentaltech.techrental.rentalorder.service.ReservationService;
import com.rentaltech.techrental.staff.model.*;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.staff.repository.SettlementRepository;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
import java.util.*;
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
    private final ContractRepository contractRepository;
    private final ContractExtensionAnnexRepository contractExtensionAnnexRepository;
    private final NotificationService notificationService;
    private final TaskRepository taskRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final ReservationService reservationService;
    private final BookingCalendarService bookingCalendarService;
    private final AllocationRepository allocationRepository;
    private final AccountRepository accountRepository;
    private final SettlementRepository settlementRepository;
    private final StaffService staffService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String STAFF_NOTIFICATION_TOPIC_TEMPLATE = "/topic/staffs/%d/notifications";

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
            invoice = processingInvoiceBuilder(rentalOrder, invoiceType, paymentMethod, expectedSubTotal,
                            taxAmount, discountAmount, depositApplied, totalAmount)
                    .vnpayTransactionId(vnpayTransactionId)
                    .frontendSuccessUrl(request.getFrontendSuccessUrl())
                    .frontendFailureUrl(request.getFrontendFailureUrl())
                    .build();
            invoice = invoiceRepository.save(invoice);
            invoiceRepository.flush();
            String paymentUrl = buildVnpayPaymentUrl(request, invoice);
            return CreatePaymentResponse.builder()
                    .checkoutUrl(paymentUrl)
                    .orderCode(Long.parseLong(vnpayTransactionId))
                    .status("PENDING")
                    .build();
        } else {
            long payosOrderCode = generateUniquePayosOrderCode();
            invoice = processingInvoiceBuilder(rentalOrder, invoiceType, paymentMethod, expectedSubTotal,
                            taxAmount, discountAmount, depositApplied, totalAmount)
                    .payosOrderCode(payosOrderCode)
                    .build();
            invoice = invoiceRepository.save(invoice);
            invoiceRepository.flush();

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

        if (orderCode == 123L) {
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

            boolean extensionOrder = isExtensionOrder(rentalOrder);
            if (rentalOrder != null) {
                System.out.println("Updating rental order status for order " + rentalOrder);
                rentalOrder.setOrderStatus(extensionOrder ? OrderStatus.IN_USE : OrderStatus.DELIVERY_CONFIRMED);
                rentalOrderRepository.save(rentalOrder);
                rentalOrderRepository.flush();
                reservationService.markConfirmed(rentalOrder.getOrderId());
                createBookingsForOrder(rentalOrder);
                updateContractStatusAfterPayment(rentalOrder);
            }

            boolean transactionCreated = false;
            if (!transactionRepository.existsByInvoice(invoice)) {
                BigDecimal transactionAmount = Optional.ofNullable(invoice.getTotalAmount()).orElse(BigDecimal.ZERO);
                Transaction transaction = buildTransaction(transactionAmount, TrasactionType.TRANSACTION_IN, invoice, null);
                transactionRepository.saveAndFlush(transaction);
                transactionCreated = true;
            }

            if (transactionCreated && rentalOrder != null) {
                notifyPaymentSuccess(rentalOrder);
                if (!extensionOrder) {
                    createDeliveryTaskIfNeeded(rentalOrder);
                }
            }

        } else {
            invoice.setPaymentDate(null);
        }

        invoiceRepository.save(invoice);
        invoiceRepository.flush();
    }


    private void notifyPaymentSuccess(RentalOrder rentalOrder) {
        if (rentalOrder.getCustomer() == null || rentalOrder.getCustomer().getCustomerId() == null) {
            return;
        }
        String title = "Thanh toán thành công";
        String message = String.format("Đơn thuê #%d đã được xác nhận thanh toán.", rentalOrder.getOrderId());
        notificationService.notifyAccount(
                rentalOrder.getCustomer().getAccount().getAccountId(),
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
                .description(String.format("Chuẩn bị giao đơn hàng #%d cho khách.", rentalOrder.getOrderId()))
                .plannedStart(LocalDateTime.now())
                .status(TaskStatus.PENDING)
                .build();
        taskRepository.save(deliveryTask);
        taskRepository.flush();
    }

    private void createBookingsForOrder(RentalOrder rentalOrder) {
        if (rentalOrder == null || rentalOrder.getOrderId() == null) {
            return;
        }
        List<Allocation> allocations = allocationRepository.findByOrderDetail_RentalOrder_OrderId(rentalOrder.getOrderId());
        if (allocations == null || allocations.isEmpty()) {
            log.debug("No allocations found to create bookings for order {}", rentalOrder.getOrderId());
            return;
        }
        bookingCalendarService.createBookingsForAllocations(allocations);
    }

    private boolean isExtensionOrder(RentalOrder rentalOrder) {
        if (rentalOrder == null) {
            return false;
        }
        if (rentalOrder.getParentOrder() != null) {
            return true;
        }
        return rentalOrder.isExtended();
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

        Account requester = Optional.ofNullable(accountRepository.findByUsername(username))
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản người dùng hiện tại"));

        Role requesterRole = requester.getRole();
        if (requesterRole == Role.OPERATOR || requesterRole == Role.CUSTOMER_SUPPORT_STAFF) {
            return InvoiceResponseDto.from(invoice);
        }

        if (requesterRole == Role.CUSTOMER) {
            String ownerUsername = rentalOrder.getCustomer().getAccount().getUsername();
            if (ownerUsername == null || !ownerUsername.equalsIgnoreCase(username)) {
                throw new AccessDeniedException("Bạn không thể xem hóa đơn của đơn hàng này");
            }
            return InvoiceResponseDto.from(invoice);
        }

        throw new AccessDeniedException("Bạn không có quyền truy cập hóa đơn này");
    }

    private BigDecimal safeSum(BigDecimal first, BigDecimal second) {
        BigDecimal left = Optional.ofNullable(first).orElse(BigDecimal.ZERO);
        BigDecimal right = Optional.ofNullable(second).orElse(BigDecimal.ZERO);
        return left.add(right);
    }

    private void notifyCustomerDepositRefund(RentalOrder order) {
        if (order == null || order.getCustomer() == null || order.getCustomer().getCustomerId() == null) {
            return;
        }
        notificationService.notifyAccount(
                order.getCustomer().getAccount().getAccountId(),
                NotificationType.ORDER_COMPLETED,
                "Đơn hàng đã hoàn tất",
                "Đã hoàn tiền cọc cho đơn hàng #" + order.getOrderId()
        );
    }

    private void notifyOperatorsDepositRefund(Long orderId, Long invoiceId) {
        if (orderId == null) {
            return;
        }
        List<Staff> operators = staffService.getStaffByRole(StaffRole.OPERATOR);
        if (operators == null || operators.isEmpty()) {
            return;
        }
        OperatorDepositRefundNotification payload = new OperatorDepositRefundNotification(
                orderId,
                invoiceId,
                "Đơn hàng #" + orderId + " đã được hoàn cọc thành công."
        );
        operators.stream()
                .filter(Objects::nonNull)
                .forEach(staff -> {
                    if (staff.getAccount() != null && staff.getAccount().getAccountId() != null) {
                        notificationService.notifyAccount(
                                staff.getAccount().getAccountId(),
                                NotificationType.ORDER_COMPLETED,
                                "Đơn hàng đã hoàn cọc",
                                payload.message()
                        );
                    }
                    Long staffId = staff.getStaffId();
                    if (staffId == null) {
                        return;
                    }
                    String destination = String.format(STAFF_NOTIFICATION_TOPIC_TEMPLATE, staffId);
                    try {
                        messagingTemplate.convertAndSend(destination, payload);
                    } catch (Exception ex) {
                        log.warn("Không thể gửi thông báo hoàn cọc tới operator {}: {}", staffId, ex.getMessage());
                    }
                });
    }

    private record OperatorDepositRefundNotification(Long orderId, Long invoiceId, String message) {}

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

        // Use returnUrl from request if provided and valid, otherwise use return URL from config
        String returnUrl = (request.getReturnUrl() != null
                && !request.getReturnUrl().trim().isEmpty()
                && !request.getReturnUrl().equals("string"))
                ? request.getReturnUrl().trim()
                : vnpayConfig.getReturnUrl(); // Use return URL from config
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

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getAllTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(TransactionResponseDto::from)
                .toList();
    }

    @Override
    @Transactional
    public InvoiceResponseDto confirmDepositRefund(Long settlementId, String username) {
        Account account = Optional.ofNullable(accountRepository.findByUsername(username))
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản xác thực"));
        Role role = account.getRole();
        if (role != Role.ADMIN && role != Role.OPERATOR && role != Role.TECHNICIAN && role != Role.CUSTOMER_SUPPORT_STAFF) {
            throw new AccessDeniedException("Không có quyền xác nhận hoàn cọc");
        }

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy settlement: " + settlementId));
        RentalOrder order = settlement.getRentalOrder();
        if (order == null) {
            throw new IllegalStateException("Settlement chưa gắn với đơn thuê");
        }

        BigDecimal subTotal = defaultZero(settlement.getFinalReturnAmount());
        BigDecimal depositApplied = defaultZero(settlement.getTotalDeposit());

        Invoice invoice = Invoice.builder()
                .rentalOrder(order)
                .invoiceType(InvoiceType.DEPOSIT_REFUND)
                .paymentMethod(PaymentMethod.BANK_ACCOUNT)
                .subTotal(subTotal)
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(subTotal)
                .depositApplied(depositApplied)
                .invoiceStatus(InvoiceStatus.SUCCEEDED)
                .paymentDate(LocalDateTime.now())
                .issueDate(LocalDateTime.now())
                .build();
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Đơn đã hoàn trả cọc => chuyển trạng thái đơn sang COMPLETED
        order.setOrderStatus(OrderStatus.COMPLETED);
        rentalOrderRepository.save(order);

        settlement.setState(SettlementState.Closed);
        if (settlement.getIssuedAt() == null) {
            settlement.setIssuedAt(LocalDateTime.now());
        }
        settlementRepository.save(settlement);

        transactionRepository.save(buildTransaction(subTotal, TrasactionType.TRANSACTION_OUT, savedInvoice, username));

        notifyCustomerDepositRefund(order);
        notifyOperatorsDepositRefund(order.getOrderId(), savedInvoice.getInvoiceId());
        createPostRentalQcTask(order);

        return InvoiceResponseDto.from(savedInvoice);
    }

    private void createPostRentalQcTask(RentalOrder order) {
        if (order == null || order.getOrderId() == null) {
            return;
        }
        Optional<TaskCategory> categoryOpt = taskCategoryRepository.findByNameIgnoreCase("Post rental QC");
        if (categoryOpt.isEmpty()) {
            log.warn("Không tìm thấy task category Post rental QC nên không thể tạo task");
            return;
        }
        Task task = Task.builder()
                .taskCategory(categoryOpt.get())
                .orderId(order.getOrderId())
                .description("QC sau thuê sau khi hoàn cọc")
                .plannedStart(LocalDateTime.now())
                .plannedEnd(LocalDateTime.now().plusHours(6))
                .status(TaskStatus.PENDING)
                .build();
        taskRepository.save(task);
    }

    @Transactional
    public void handleVnpayCallback(Map<String, String> params) {
        log.info("=== STARTING VNPAY CALLBACK PROCESSING ===");
        String vnp_SecureHash = params.get("vnp_SecureHash");
        String vnp_TxnRef = params.get("vnp_TxnRef");
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String vnp_TransactionStatus = params.get("vnp_TransactionStatus");

        log.info("VNPAY callback params received: {}", params);
        log.info("vnp_TxnRef: {}", vnp_TxnRef);
        log.info("vnp_ResponseCode: {}", vnp_ResponseCode);
        log.info("vnp_TransactionStatus: {}", vnp_TransactionStatus);
        log.info("VNPAY vnp_SecureHash from request: {}", vnp_SecureHash);

        Map<String, String> paramsForHash = new HashMap<>(params);
        // Log params before hashing for debugging
        log.info("VNPAY params for hash calculation: {}", paramsForHash);
        // When validating callback, use raw values (already decoded by servlet), don't encode again
        String secureHash = VnpayUtil.hashAllFields(paramsForHash, vnpayConfig.getHashSecret(), false);

        log.info("VNPAY calculated hash: {}", secureHash);
        log.info("VNPAY received hash: {}", vnp_SecureHash);
        log.info("VNPAY hash secret used: {}", vnpayConfig.getHashSecret());
        log.info("VNPAY hash match: {}", secureHash.equals(vnp_SecureHash));

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
            boolean extensionOrder = isExtensionOrder(rentalOrder);
            if (rentalOrder != null) {
                log.info("VNPAY payment succeeded for order {} - updating status", rentalOrder.getOrderId());
                rentalOrder.setOrderStatus(extensionOrder ? OrderStatus.IN_USE : OrderStatus.DELIVERY_CONFIRMED);
                rentalOrderRepository.save(rentalOrder);
                rentalOrderRepository.flush(); // Ensure order status is persisted before proceeding
                log.info("VNPAY order {} status updated to DELIVERY_CONFIRMED successfully", rentalOrder.getOrderId());

                // Wrap auxiliary operations in try-catch to prevent rollback if they fail
                try {
                    reservationService.markConfirmed(rentalOrder.getOrderId());
                } catch (Exception e) {
                    log.error("Failed to mark reservations as confirmed for order {}: {}", rentalOrder.getOrderId(), e.getMessage(), e);
                }

                try {
                    createBookingsForOrder(rentalOrder);
                } catch (Exception e) {
                    log.error("Failed to create bookings for order {}: {}", rentalOrder.getOrderId(), e.getMessage(), e);
                }
                try {
                    updateContractStatusAfterPayment(rentalOrder);
                } catch (Exception e) {
                    log.error("Failed to update contract status for order {}: {}", rentalOrder.getOrderId(), e.getMessage(), e);
                }
            } else {
                log.warn("VNPAY payment succeeded but rentalOrder is null for invoice {}", invoice.getInvoiceId());
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
                transactionRepository.flush();
                transactionCreated = true;
            }

            if (transactionCreated && rentalOrder != null) {
                try {
                    notifyPaymentSuccess(rentalOrder);
                } catch (Exception e) {
                    log.error("Failed to send payment success notification for order {}: {}", rentalOrder.getOrderId(), e.getMessage(), e);
                }

                if (!extensionOrder) {
                    try {
                        createDeliveryTaskIfNeeded(rentalOrder);
                    } catch (Exception e) {
                        log.error("Failed to create delivery task for order {}: {}", rentalOrder.getOrderId(), e.getMessage(), e);
                    }
                }
            }
        } else {
            invoice.setPaymentDate(null);
        }

        invoiceRepository.save(invoice);
        invoiceRepository.flush(); // Ensure invoice status is persisted

        log.info("=== VNPAY CALLBACK PROCESSING COMPLETED ===");
        log.info("Invoice {} status: {}", invoice.getInvoiceId(), invoice.getInvoiceStatus());
        if (invoice.getRentalOrder() != null) {
            log.info("Order {} status: {}", invoice.getRentalOrder().getOrderId(), invoice.getRentalOrder().getOrderStatus());
        }
    }

    private Invoice.InvoiceBuilder processingInvoiceBuilder(RentalOrder rentalOrder,
                                                            InvoiceType invoiceType,
                                                            PaymentMethod paymentMethod,
                                                            BigDecimal subTotal,
                                                            BigDecimal taxAmount,
                                                            BigDecimal discountAmount,
                                                            BigDecimal depositApplied,
                                                            BigDecimal totalAmount) {
        return Invoice.builder()
                .rentalOrder(rentalOrder)
                .invoiceType(invoiceType)
                .paymentMethod(paymentMethod)
                .paymentDate(null)
                .subTotal(subTotal)
                .taxAmount(taxAmount)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .depositApplied(depositApplied)
                .dueDate(LocalDateTime.now().plusDays(3))
                .invoiceStatus(InvoiceStatus.PROCESSING)
                .pdfUrl(null)
                .issueDate(null);
    }

    private Transaction buildTransaction(BigDecimal amount, TrasactionType type, Invoice invoice, String createdBy) {
        return Transaction.builder()
                .amount(amount)
                .transactionType(type)
                .invoice(invoice)
                .createdBy(createdBy)
                .build();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private void updateContractStatusAfterPayment(RentalOrder rentalOrder) {
        if (rentalOrder == null || rentalOrder.getOrderId() == null) {
            return;
        }
        if (isExtensionOrder(rentalOrder)) {
            contractExtensionAnnexRepository.findFirstByExtensionOrder_OrderId(rentalOrder.getOrderId())
                    .ifPresent(annex -> {
                        annex.setStatus(ContractStatus.ACTIVE);
                        contractExtensionAnnexRepository.save(annex);
                    });
        } else {
            contractRepository.findFirstByOrderIdOrderByCreatedAtDesc(rentalOrder.getOrderId())
                    .ifPresent(contract -> {
                        contract.setStatus(ContractStatus.ACTIVE);
                        contract.setSignedAt(Optional.ofNullable(contract.getSignedAt()).orElse(LocalDateTime.now()));
                        contractRepository.save(contract);
                    });
        }
    }
}
