package com.rentaltech.techrental.finance.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.repository.AccountRepository;
import com.rentaltech.techrental.contract.repository.ContractExtensionAnnexRepository;
import com.rentaltech.techrental.contract.repository.ContractRepository;
import com.rentaltech.techrental.device.repository.AllocationRepository;
import com.rentaltech.techrental.finance.config.VnpayConfig;
import com.rentaltech.techrental.finance.model.*;
import com.rentaltech.techrental.finance.model.dto.CreatePaymentRequest;
import com.rentaltech.techrental.finance.model.dto.InvoiceResponseDto;
import com.rentaltech.techrental.finance.repository.InvoiceRepository;
import com.rentaltech.techrental.finance.repository.TransactionRepository;
import com.rentaltech.techrental.finance.util.VnpayUtil;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.rentalorder.service.BookingCalendarService;
import com.rentaltech.techrental.rentalorder.service.ReservationService;
import com.rentaltech.techrental.staff.model.*;
import com.rentaltech.techrental.staff.repository.SettlementRepository;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PayOS payOS;
    @Mock
    private VnpayConfig vnpayConfig;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private RentalOrderRepository rentalOrderRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private ContractExtensionAnnexRepository contractExtensionAnnexRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskCategoryRepository taskCategoryRepository;
    @Mock
    private ReservationService reservationService;
    @Mock
    private BookingCalendarService bookingCalendarService;
    @Mock
    private AllocationRepository allocationRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private StaffService staffService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "payosReturnUrl", "http://return");
        ReflectionTestUtils.setField(paymentService, "payosCancelUrl", "http://cancel");
    }

    @Test
    void createPaymentRejectsUnsupportedInvoiceType() {
        CreatePaymentRequest request = baseRequest();
        request.setInvoiceType(InvoiceType.DEPOSIT_REFUND);

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void createPaymentRejectsUnsupportedMethod() {
        CreatePaymentRequest request = baseRequest();
        request.setPaymentMethod(PaymentMethod.MOMO);

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void createPaymentValidatesAmountMatchesOrder() {
        CreatePaymentRequest request = baseRequest();
        request.setAmount(BigDecimal.valueOf(400));
        RentalOrder order = baseOrder(1L);
        when(rentalOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.createPayment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Số tiền không khớp");
    }

    @Test
    void handleWebhookSkipsConnectivityPing() {
        WebhookData data = mock(WebhookData.class);
        when(data.getOrderCode()).thenReturn(123L);

        paymentService.handleWebhook(data);

        verify(invoiceRepository, never()).findByPayosOrderCode(anyLong());
    }

    @Test
    void handleWebhookProcessesSuccessfulPayment() {
        stubDeliveryTaskDependencies();

        WebhookData data = mock(WebhookData.class);
        when(data.getOrderCode()).thenReturn(999L);
        when(data.getCode()).thenReturn("00");
        when(data.getDesc()).thenReturn("success");
        when(data.getAmount()).thenReturn(1000L);

        RentalOrder order = baseOrder(10L);
        Invoice invoice = Invoice.builder()
                .invoiceId(20L)
                .payosOrderCode(999L)
                .totalAmount(BigDecimal.valueOf(300))
                .rentalOrder(order)
                .invoiceStatus(InvoiceStatus.PROCESSING)
                .build();
        when(invoiceRepository.findByPayosOrderCode(999L)).thenReturn(Optional.of(invoice));
        when(transactionRepository.existsByInvoice(invoice)).thenReturn(false);
        when(contractRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getOrderId())).thenReturn(Optional.empty());

        paymentService.handleWebhook(data);

        assertThat(invoice.getInvoiceStatus()).isEqualTo(InvoiceStatus.SUCCEEDED);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.DELIVERY_CONFIRMED);
        verify(transactionRepository).saveAndFlush(any(Transaction.class));
    }

    @Test
    void handlePayOsReturnReturnsFalseWhenOrderCodeMissing() {
        assertThat(paymentService.handlePayOsReturn(null, true)).isFalse();
        verify(invoiceRepository, never()).findByPayosOrderCode(anyLong());
    }

    @Test
    void getInvoiceForCustomerAllowsOwner() {
        RentalOrder order = baseOrder(5L);
        when(rentalOrderRepository.findById(5L)).thenReturn(Optional.of(order));
        Account owner = Account.builder()
                .accountId(1L)
                .username("owner")
                .role(Role.CUSTOMER)
                .build();
        when(accountRepository.findByUsername("owner")).thenReturn(owner);
        Invoice invoice = Invoice.builder().invoiceId(1L).build();
        when(invoiceRepository.findByRentalOrder_OrderIdOrderByInvoiceIdDesc(5L)).thenReturn(List.of(invoice));

        List<InvoiceResponseDto> result = paymentService.getInvoiceForCustomer(5L, "owner");

        assertThat(result).hasSize(1);
    }

    @Test
    void getInvoiceForCustomerRejectsNonOwnerCustomer() {
        RentalOrder order = baseOrder(5L);
        when(rentalOrderRepository.findById(5L)).thenReturn(Optional.of(order));
        Account other = Account.builder()
                .accountId(2L)
                .username("other")
                .role(Role.CUSTOMER)
                .build();
        when(accountRepository.findByUsername("other")).thenReturn(other);

        assertThatThrownBy(() -> paymentService.getInvoiceForCustomer(5L, "other"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void confirmDepositRefundCreatesSuccessInvoice() {
        Account operator = Account.builder()
                .accountId(1L)
                .username("operator")
                .role(Role.OPERATOR)
                .build();
        when(accountRepository.findByUsername("operator")).thenReturn(operator);

        RentalOrder order = baseOrder(7L);
        Settlement settlement = Settlement.builder()
                .settlementId(3L)
                .rentalOrder(order)
                .damageFee(BigDecimal.TEN)
                .lateFee(BigDecimal.ZERO)
                .totalDeposit(BigDecimal.valueOf(50))
                .finalReturnAmount(BigDecimal.valueOf(40))
                .state(SettlementState.Issued)
                .build();
        when(settlementRepository.findById(3L)).thenReturn(Optional.of(settlement));
        when(imageStorageService.uploadInvoiceProof(any(MultipartFile.class), eq(3L))).thenReturn("proof-url");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice invc = inv.getArgument(0);
            invc.setInvoiceId(99L);
            return invc;
        });
        when(staffService.getStaffByRole(StaffRole.OPERATOR)).thenReturn(List.of(
                Staff.builder()
                        .staffId(15L)
                        .staffRole(StaffRole.OPERATOR)
                        .account(Account.builder().accountId(77L).username("staff").build())
                        .build()
        ));
        TaskCategory qcCategory = TaskCategory.builder().taskCategoryId(22L).name("Post rental QC").build();
        when(taskCategoryRepository.findByNameIgnoreCase("Post rental QC")).thenReturn(Optional.of(qcCategory));

        MockMultipartFile proof = new MockMultipartFile("proof", "proof.png", "image/png", new byte[]{1, 2});

        InvoiceResponseDto response = paymentService.confirmDepositRefund(3L, "operator", proof);

        assertThat(response.getInvoiceId()).isEqualTo(99L);
        verify(transactionRepository).save(any(Transaction.class));
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void handleVnpayCallbackRejectsInvalidHash() {
        stubVnpayHashSecret();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "abc");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_SecureHash", "invalid");

        assertThatThrownBy(() -> paymentService.handleVnpayCallback(params))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VNPAY");
    }

    @Test
    void handleVnpayCallbackUpdatesInvoiceOnSuccess() {
        stubVnpayHashSecret();
        stubDeliveryTaskDependencies();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "txn1");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        String hash = VnpayUtil.hashAllFields(new HashMap<>(params), "secret", false);
        params.put("vnp_SecureHash", hash);

        RentalOrder order = baseOrder(12L);
        Invoice invoice = Invoice.builder()
                .invoiceId(50L)
                .vnpayTransactionId("txn1")
                .totalAmount(BigDecimal.valueOf(200))
                .rentalOrder(order)
                .invoiceStatus(InvoiceStatus.PROCESSING)
                .build();
        when(invoiceRepository.findByVnpayTransactionId("txn1")).thenReturn(Optional.of(invoice));
        when(contractRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getOrderId())).thenReturn(Optional.empty());
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        paymentService.handleVnpayCallback(params);

        assertThat(invoice.getInvoiceStatus()).isEqualTo(InvoiceStatus.SUCCEEDED);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.DELIVERY_CONFIRMED);
        verify(transactionRepository).save(any(Transaction.class));
    }

    private void stubDeliveryTaskDependencies() {
        TaskCategory deliveryCategory = TaskCategory.builder()
                .taskCategoryId(101L)
                .name("DELIVERY")
                .build();
        when(taskCategoryRepository.findByNameIgnoreCase("DELIVERY")).thenReturn(Optional.of(deliveryCategory));
        when(taskRepository.findByOrderId(anyLong())).thenReturn(Collections.emptyList());
    }

    private void stubVnpayHashSecret() {
        when(vnpayConfig.getHashSecret()).thenReturn("secret");
    }

    private CreatePaymentRequest baseRequest() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId(1L);
        request.setInvoiceType(InvoiceType.RENT_PAYMENT);
        request.setPaymentMethod(PaymentMethod.PAYOS);
        request.setAmount(BigDecimal.valueOf(300));
        request.setDescription("Rent");
        request.setReturnUrl("http://return");
        request.setCancelUrl("http://cancel");
        request.setFrontendSuccessUrl("http://success");
        request.setFrontendFailureUrl("http://fail");
        return request;
    }

    private RentalOrder baseOrder(long orderId) {
        Account customerAccount = Account.builder()
                .accountId(orderId * 2)
                .username("owner")
                .role(Role.CUSTOMER)
                .build();
        Customer customer = Customer.builder()
                .customerId(orderId * 3)
                .account(customerAccount)
                .build();
        return RentalOrder.builder()
                .orderId(orderId)
                .customer(customer)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(3))
                .orderStatus(OrderStatus.PENDING)
                .depositAmount(BigDecimal.valueOf(100))
                .depositAmountHeld(BigDecimal.ZERO)
                .totalPrice(BigDecimal.valueOf(200))
                .pricePerDay(BigDecimal.TEN)
                .build();
    }
}
