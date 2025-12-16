package com.rentaltech.techrental.finance.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.repository.AccountRepository;
import com.rentaltech.techrental.contract.model.ContractExtensionAnnex;
import com.rentaltech.techrental.contract.model.ContractStatus;
import com.rentaltech.techrental.contract.repository.ContractExtensionAnnexRepository;
import com.rentaltech.techrental.contract.repository.ContractRepository;
import com.rentaltech.techrental.device.model.Allocation;
import com.rentaltech.techrental.device.repository.AllocationRepository;
import com.rentaltech.techrental.finance.config.VnpayConfig;
import com.rentaltech.techrental.finance.model.*;
import com.rentaltech.techrental.finance.model.dto.CreatePaymentRequest;
import com.rentaltech.techrental.finance.model.dto.CreatePaymentResponse;
import com.rentaltech.techrental.finance.model.dto.InvoiceResponseDto;
import com.rentaltech.techrental.finance.model.dto.TransactionResponseDto;
import com.rentaltech.techrental.finance.repository.InvoiceRepository;
import com.rentaltech.techrental.finance.repository.TransactionRepository;
import com.rentaltech.techrental.finance.util.VnpayUtil;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.model.RentalOrderExtension;
import com.rentaltech.techrental.rentalorder.model.RentalOrderExtensionStatus;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderExtensionRepository;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.rentalorder.service.BookingCalendarService;
import com.rentaltech.techrental.rentalorder.service.ReservationService;
import com.rentaltech.techrental.staff.model.*;
import com.rentaltech.techrental.staff.repository.SettlementRepository;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
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
    private final RentalOrderExtensionRepository rentalOrderExtensionRepository;
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
    private final ImageStorageService imageStorageService;

    @Value("${payos.return-url:http://localhost:8080/api/v1/payos/return}")
    private String payosReturnUrl;

    @Value("${payos.cancel-url:http://localhost:8080/api/v1/payos/cancel}")
    private String payosCancelUrl;

    private static final String STAFF_NOTIFICATION_TOPIC_TEMPLATE = "/topic/staffs/%d/notifications";

    @Override
    @Transactional
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        InvoiceType invoiceType = request.getInvoiceType();
        if (invoiceType != InvoiceType.RENT_PAYMENT) {
            throw new UnsupportedOperationException("Hiện chưa hỗ trợ loại hóa đơn: " + invoiceType);
        }

        PaymentMethod paymentMethod = request.getPaymentMethod();
        if (paymentMethod == PaymentMethod.MOMO || paymentMethod == PaymentMethod.BANK_ACCOUNT) {
            throw new UnsupportedOperationException("Hiện chưa hỗ trợ phương thức thanh toán: " + paymentMethod);
        }

        if (paymentMethod != PaymentMethod.PAYOS && paymentMethod != PaymentMethod.VNPAY) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ: " + paymentMethod);
        }

        RentalOrder rentalOrder = rentalOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn thuê: " + request.getOrderId()));

        RentalOrderExtension extension = null;
        ContractExtensionAnnex annex = null;
        Invoice existingAnnexInvoice = null;
        if (request.getExtensionId() != null) {
            extension = rentalOrderExtensionRepository.findById(request.getExtensionId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bản gia hạn: " + request.getExtensionId()));
            if (extension.getRentalOrder() == null
                    || extension.getRentalOrder().getOrderId() == null
                    || !extension.getRentalOrder().getOrderId().equals(rentalOrder.getOrderId())) {
                throw new IllegalArgumentException("Bản gia hạn không thuộc đơn thuê " + request.getOrderId());
            }
            Long extensionId = extension.getExtensionId();
            annex = contractExtensionAnnexRepository.findFirstByRentalOrderExtension_ExtensionId(extensionId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phụ lục gia hạn cho extension " + extensionId));
            existingAnnexInvoice = annex.getInvoice();
            if (existingAnnexInvoice != null && existingAnnexInvoice.getInvoiceStatus() == InvoiceStatus.SUCCEEDED) {
                throw new IllegalStateException("Phụ lục gia hạn đã được thanh toán");
            }
        }

        BigDecimal expectedSubTotal = extension != null
                ? defaultZero(extension.getAdditionalPrice())
                : safeSum(rentalOrder.getDepositAmount(), rentalOrder.getTotalPrice());
        if (request.getAmount() == null || request.getAmount().compareTo(expectedSubTotal) != 0) {
            throw new IllegalArgumentException("Số tiền thanh toán không khớp với giá trị yêu cầu");
        }
        if (expectedSubTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá trị thanh toán không hợp lệ");
        }

        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal depositApplied = BigDecimal.ZERO;
        BigDecimal totalAmount = expectedSubTotal.add(taxAmount).subtract(discountAmount);

        boolean reuseExistingInvoice = existingAnnexInvoice != null;
        Invoice invoice;
        if (paymentMethod == PaymentMethod.VNPAY) {
            String vnpayTransactionId = generateVnpayTransactionId();
            if (reuseExistingInvoice) {
                invoice = resetInvoiceForPayment(existingAnnexInvoice, paymentMethod, expectedSubTotal,
                        taxAmount, discountAmount, depositApplied, totalAmount,
                        request.getFrontendSuccessUrl(), request.getFrontendFailureUrl());
                invoice.setVnpayTransactionId(vnpayTransactionId);
                invoice.setPayosOrderCode(null);
            } else {
                invoice = processingInvoiceBuilder(rentalOrder, invoiceType, paymentMethod, expectedSubTotal,
                                taxAmount, discountAmount, depositApplied, totalAmount)
                        .vnpayTransactionId(vnpayTransactionId)
                        .frontendSuccessUrl(request.getFrontendSuccessUrl())
                        .frontendFailureUrl(request.getFrontendFailureUrl())
                        .build();
            }
            invoice = invoiceRepository.save(invoice);
            invoiceRepository.flush();
            if (extension != null && !reuseExistingInvoice) {
                attachInvoiceToAnnex(extension, invoice);
            }
            String paymentUrl = buildVnpayPaymentUrl(request, invoice);
            return CreatePaymentResponse.builder()
                    .checkoutUrl(paymentUrl)
                    .orderCode(Long.parseLong(vnpayTransactionId))
                    .status("PENDING")
                    .build();
        } else {
            long payosOrderCode = generateUniquePayosOrderCode();
            if (reuseExistingInvoice) {
                invoice = resetInvoiceForPayment(existingAnnexInvoice, paymentMethod, expectedSubTotal,
                        taxAmount, discountAmount, depositApplied, totalAmount,
                        request.getFrontendSuccessUrl(), request.getFrontendFailureUrl());
                invoice.setPayosOrderCode(payosOrderCode);
                invoice.setVnpayTransactionId(null);
            } else {
                invoice = processingInvoiceBuilder(rentalOrder, invoiceType, paymentMethod, expectedSubTotal,
                                taxAmount, discountAmount, depositApplied, totalAmount)
                        .payosOrderCode(payosOrderCode)
                        .frontendSuccessUrl(request.getFrontendSuccessUrl())
                        .frontendFailureUrl(request.getFrontendFailureUrl())
                        .build();
            }
            invoice = invoiceRepository.save(invoice);
            invoiceRepository.flush();
            if (extension != null && !reuseExistingInvoice) {
                attachInvoiceToAnnex(extension, invoice);
            }

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
                throw new IllegalStateException("Không thể tạo liên kết thanh toán PayOS", ex);
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

        boolean success = "00".equals(code);
        applyPayosPaymentResult(orderCode, success);
    }

    @Override
    @Transactional
    public boolean handlePayOsReturn(Long orderCode, boolean success) {
        if (orderCode == null) {
            log.warn("PayOS return URL called without orderCode");
            return false;
        }
        applyPayosPaymentResult(orderCode, success);
        return success;
    }

    private void applyPayosPaymentResult(Long orderCode, boolean success) {
        Invoice invoice = invoiceRepository.findByPayosOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn tương ứng với order code PayOS: " + orderCode));
        RentalOrder rentalOrder = invoice.getRentalOrder();
        InvoiceStatus newStatus = success ? InvoiceStatus.SUCCEEDED : InvoiceStatus.FAILED;
        invoice.setInvoiceStatus(newStatus);

        if (success) {
            invoice.setPaymentDate(LocalDateTime.now());

            boolean extensionOrder = rentalOrder != null && rentalOrder.getOrderStatus() == OrderStatus.IN_USE;
            if (rentalOrder != null) {
                log.info("Updating rental order status for order {}", rentalOrder.getOrderId());
                rentalOrder.setOrderStatus(extensionOrder ? OrderStatus.IN_USE : OrderStatus.DELIVERY_CONFIRMED);
                rentalOrderRepository.save(rentalOrder);
                rentalOrderRepository.flush();
                reservationService.markConfirmed(rentalOrder.getOrderId());
                createBookingsForOrder(rentalOrder);
                syncAnnexBookings(invoice);
                updateContractStatusAfterPayment(rentalOrder);
                updateExtensionStatusAfterPayment(invoice);
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

    private CreatePaymentLinkRequest buildCreatePaymentRequest(CreatePaymentRequest request, Invoice invoice) {
        String providedReturnUrl = StringUtils.hasText(request.getReturnUrl()) && !"string".equalsIgnoreCase(request.getReturnUrl().trim())
                ? request.getReturnUrl().trim()
                : null;
        String providedCancelUrl = StringUtils.hasText(request.getCancelUrl()) && !"string".equalsIgnoreCase(request.getCancelUrl().trim())
                ? request.getCancelUrl().trim()
                : null;

        String effectiveReturnUrl = StringUtils.hasText(providedReturnUrl)
                ? providedReturnUrl
                : payosReturnUrl;
        String effectiveCancelUrl = StringUtils.hasText(providedCancelUrl)
                ? providedCancelUrl
                : payosCancelUrl;

        if (!StringUtils.hasText(effectiveReturnUrl)) {
            throw new IllegalStateException("URL trả về chưa được cấu hình cho PayOS");
        }
        if (!StringUtils.hasText(effectiveCancelUrl)) {
            throw new IllegalStateException("URL hủy chưa được cấu hình cho PayOS");
        }

        PaymentLinkItem itemData = PaymentLinkItem.builder()
                .name("Đơn hàng TechRent: " + invoice.getRentalOrder().getOrderId())
                .quantity(1)
                .price(invoice.getTotalAmount().longValueExact())
                .build();

        return CreatePaymentLinkRequest.builder()
                .orderCode(invoice.getPayosOrderCode())
                .amount(request.getAmount().longValueExact())
                .description("Thanh toán")
                .returnUrl(effectiveReturnUrl)
                .cancelUrl(effectiveCancelUrl)
                .item(itemData)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponseDto> getInvoiceForCustomer(Long rentalOrderId, String username) {
        RentalOrder rentalOrder = rentalOrderRepository.findById(rentalOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + rentalOrderId));

        Account requester = Optional.ofNullable(accountRepository.findByUsername(username))
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản người dùng hiện tại"));

        Role requesterRole = requester.getRole();
        if (requesterRole == Role.CUSTOMER) {
            String ownerUsername = Optional.ofNullable(rentalOrder.getCustomer())
                    .map(customer -> customer.getAccount())
                    .map(Account::getUsername)
                    .orElse(null);
            if (ownerUsername == null || !ownerUsername.equalsIgnoreCase(username)) {
                throw new AccessDeniedException("Bạn không thể xem hóa đơn của đơn hàng này");
            }
        } else if (requesterRole != Role.OPERATOR && requesterRole != Role.CUSTOMER_SUPPORT_STAFF && requesterRole != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền truy cập hóa đơn này");
        }

        List<InvoiceResponseDto> invoices = invoiceRepository.findByRentalOrder_OrderIdOrderByInvoiceIdDesc(rentalOrderId).stream()
                .map(InvoiceResponseDto::from)
                .toList();
        if (invoices.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy hóa đơn cho đơn hàng: " + rentalOrderId);
        }
        return invoices;
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
            throw new IllegalStateException("Không thể sinh chữ ký bảo mật cho VNPAY");
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
    @Transactional(readOnly = true)
    public List<InvoiceResponseDto> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .sorted(Comparator.comparing(Invoice::getInvoiceId).reversed())
                .map(InvoiceResponseDto::from)
                .toList();
    }

    @Override
    @Transactional
    public InvoiceResponseDto confirmDepositRefund(Long settlementId, String username, MultipartFile proofFile) {
        Account account = Optional.ofNullable(accountRepository.findByUsername(username))
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản xác thực"));
        Role role = account.getRole();
        if (role != Role.ADMIN && role != Role.OPERATOR && role != Role.TECHNICIAN && role != Role.CUSTOMER_SUPPORT_STAFF) {
            throw new AccessDeniedException("Không có quyền xác nhận hoàn cọc");
        }

        if (proofFile == null || proofFile.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng cung cấp file bằng chứng hoàn cọc");
        }

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy settlement: " + settlementId));
        RentalOrder order = settlement.getRentalOrder();
        if (order == null) {
            throw new IllegalStateException("Biên bản thanh lý (settlement) chưa gắn với đơn thuê");
        }

        BigDecimal damageFee = defaultZero(settlement.getDamageFee());
        BigDecimal lateFee = defaultZero(settlement.getLateFee());
        BigDecimal subTotal = damageFee.add(lateFee);
        BigDecimal depositApplied = defaultZero(settlement.getTotalDeposit());
        String proofUrl = imageStorageService.uploadInvoiceProof(proofFile, settlementId);
        BigDecimal totalAmount = defaultZero(settlement.getFinalReturnAmount());
        InvoiceType invoiceType = totalAmount.compareTo(BigDecimal.ZERO) >= 0
                ? InvoiceType.DEPOSIT_REFUND
                : InvoiceType.COMPENSATION_PAYMENT;

        Invoice invoice = Invoice.builder()
                .rentalOrder(order)
                .invoiceType(invoiceType)
                .paymentMethod(PaymentMethod.BANK_ACCOUNT)
                .subTotal(subTotal)
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .depositApplied(depositApplied)
                .invoiceStatus(InvoiceStatus.SUCCEEDED)
                .paymentDate(LocalDateTime.now())
                .issueDate(LocalDateTime.now())
                .proofUrl(proofUrl)
                .build();
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Đơn đã hoàn trả cọc => chuyển trạng thái đơn sang COMPLETED (bao gồm các đơn extend)
        order.setOrderStatus(OrderStatus.COMPLETED);
        rentalOrderRepository.save(order);
        completeExtensionsForOrder(order);
        // Không còn đơn gia hạn riêng biệt; chỉ cập nhật trạng thái của đơn gốc

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
            throw new IllegalStateException("Chữ ký VNPAY không hợp lệ");
        }

        Invoice invoice = invoiceRepository.findByVnpayTransactionId(vnp_TxnRef)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn tương ứng giao dịch VNPAY: " + vnp_TxnRef));

        boolean isSuccess = "00".equals(vnp_ResponseCode) && "00".equals(vnp_TransactionStatus);
        InvoiceStatus newStatus = isSuccess ? InvoiceStatus.SUCCEEDED : InvoiceStatus.FAILED;

        invoice.setInvoiceStatus(newStatus);

        if (newStatus == InvoiceStatus.SUCCEEDED) {
            invoice.setPaymentDate(LocalDateTime.now());

            RentalOrder rentalOrder = invoice.getRentalOrder();
            boolean extensionOrder = rentalOrder != null && rentalOrder.getOrderStatus() == OrderStatus.IN_USE;
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
                    syncAnnexBookings(invoice);
                } catch (Exception e) {
                    log.error("Failed to sync booking calendar for annex invoice {}: {}", invoice.getInvoiceId(), e.getMessage(), e);
                }
                try {
                    updateContractStatusAfterPayment(rentalOrder);
                } catch (Exception e) {
                    log.error("Failed to update contract status for order {}: {}", rentalOrder.getOrderId(), e.getMessage(), e);
                }
                try {
                    updateExtensionStatusAfterPayment(invoice);
                } catch (Exception e) {
                    log.error("Failed to update extension status for invoice {}: {}", invoice.getInvoiceId(), e.getMessage(), e);
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
                .proofUrl(null)
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

    private void attachInvoiceToAnnex(RentalOrderExtension extension, Invoice invoice) {
        if (extension == null || invoice == null || extension.getExtensionId() == null) {
            return;
        }
        ContractExtensionAnnex annex = contractExtensionAnnexRepository
                .findFirstByRentalOrderExtension_ExtensionId(extension.getExtensionId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy phụ lục gia hạn cho extension " + extension.getExtensionId()));
        Invoice existingInvoice = annex.getInvoice();
        if (existingInvoice != null && existingInvoice.getInvoiceId() != null
                && !Objects.equals(existingInvoice.getInvoiceId(), invoice.getInvoiceId())) {
            if (existingInvoice.getInvoiceStatus() == InvoiceStatus.PROCESSING) {
                throw new IllegalStateException("Phụ lục gia hạn đang có hóa đơn chờ thanh toán");
            }
        }
        annex.setInvoice(invoice);
        contractExtensionAnnexRepository.save(annex);
    }

    private Invoice resetInvoiceForPayment(Invoice invoice,
                                           PaymentMethod paymentMethod,
                                           BigDecimal subTotal,
                                           BigDecimal taxAmount,
                                           BigDecimal discountAmount,
                                           BigDecimal depositApplied,
                                           BigDecimal totalAmount,
                                           String successUrl,
                                           String failureUrl) {
        invoice.setPaymentMethod(paymentMethod);
        invoice.setInvoiceStatus(InvoiceStatus.PROCESSING);
        invoice.setPaymentDate(null);
        invoice.setProofUrl(null);
        invoice.setSubTotal(subTotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setDiscountAmount(discountAmount);
        invoice.setTotalAmount(totalAmount);
        invoice.setDepositApplied(depositApplied);
        invoice.setDueDate(LocalDateTime.now().plusDays(3));
        invoice.setIssueDate(null);
        invoice.setFrontendSuccessUrl(successUrl);
        invoice.setFrontendFailureUrl(failureUrl);
        return invoice;
    }

    private void syncAnnexBookings(Invoice invoice) {
        if (invoice == null) {
            return;
        }
        contractExtensionAnnexRepository.findByInvoice(invoice)
                .ifPresent(annex -> {
                    RentalOrder originalOrder = annex.getRentalOrderExtension() != null ? annex.getRentalOrderExtension().getRentalOrder() : null;
                    if (originalOrder == null || originalOrder.getOrderId() == null) {
                        return;
                    }
                    bookingCalendarService.clearBookingsForOrder(originalOrder.getOrderId());
                    createBookingsForOrder(originalOrder);
                });
    }

    private void updateExtensionStatusAfterPayment(Invoice invoice) {
        if (invoice == null) {
            return;
        }
        contractExtensionAnnexRepository.findByInvoice(invoice)
                .map(ContractExtensionAnnex::getRentalOrderExtension)
                .map(this::ensureExtensionLoaded)
                .filter(Objects::nonNull)
                .ifPresent(extension -> {
                    extension.setStatus(RentalOrderExtensionStatus.IN_USE);
                    rentalOrderExtensionRepository.save(extension);
                    updateOriginalOrderAfterExtensionPaid(extension);
                });
    }

    private RentalOrderExtension ensureExtensionLoaded(RentalOrderExtension extension) {
        if (extension == null || extension.getExtensionId() == null) {
            return null;
        }
        return rentalOrderExtensionRepository.findById(extension.getExtensionId())
                .orElse(null);
    }

    private void completeExtensionsForOrder(RentalOrder order) {
        if (order == null || order.getOrderId() == null) {
            return;
        }
        List<RentalOrderExtension> inUseExtensions = rentalOrderExtensionRepository
                .findByRentalOrder_OrderIdAndStatus(order.getOrderId(), RentalOrderExtensionStatus.IN_USE);
        if (inUseExtensions.isEmpty()) {
            return;
        }
        inUseExtensions.forEach(extension -> extension.setStatus(RentalOrderExtensionStatus.COMPLETED));
        rentalOrderExtensionRepository.saveAll(inUseExtensions);
    }

    private void updateOriginalOrderAfterExtensionPaid(RentalOrderExtension extension) {
        if (extension == null) {
            return;
        }
        RentalOrder order = extension.getRentalOrder();
        if (order == null || order.getOrderId() == null) {
            return;
        }
        order.setPlanEndDate(extension.getExtensionEnd());
        int currentDuration = order.getDurationDays() != null ? order.getDurationDays() : 0;
        int addedDuration = extension.getDurationDays() != null ? extension.getDurationDays() : 0;
        order.setDurationDays(currentDuration + addedDuration);
        BigDecimal currentTotal = Optional.ofNullable(order.getTotalPrice()).orElse(BigDecimal.ZERO);
        BigDecimal extensionTotal = Optional.ofNullable(extension.getAdditionalPrice()).orElse(BigDecimal.ZERO);
        order.setTotalPrice(currentTotal.add(extensionTotal));
        rentalOrderRepository.save(order);
    }

    private void updateContractStatusAfterPayment(RentalOrder rentalOrder) {
        if (rentalOrder == null || rentalOrder.getOrderId() == null) {
            return;
        }
        if (isExtensionOrder(rentalOrder)) {
            contractExtensionAnnexRepository.findFirstByRentalOrderExtension_RentalOrder_OrderId(rentalOrder.getOrderId())
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

    private boolean isExtensionOrder(RentalOrder rentalOrder) {
        if (rentalOrder == null || rentalOrder.getOrderId() == null) {
            return false;
        }
        return contractExtensionAnnexRepository.findFirstByRentalOrderExtension_RentalOrder_OrderId(rentalOrder.getOrderId()).isPresent();
    }
}
