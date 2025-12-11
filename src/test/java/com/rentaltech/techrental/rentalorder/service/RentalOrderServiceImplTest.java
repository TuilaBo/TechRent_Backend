package com.rentaltech.techrental.rentalorder.service;

import com.rentaltech.techrental.device.repository.AllocationConditionSnapshotRepository;
import com.rentaltech.techrental.device.repository.AllocationRepository;
import com.rentaltech.techrental.device.repository.DeviceModelRepository;
import com.rentaltech.techrental.device.repository.DiscrepancyReportRepository;
import com.rentaltech.techrental.device.service.DeviceAllocationQueryService;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.BookingCalendarRepository;
import com.rentaltech.techrental.rentalorder.repository.OrderDetailRepository;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderExtensionRepository;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.staff.service.PreRentalQcTaskCreator;
import com.rentaltech.techrental.staff.service.staffservice.StaffService;
import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.KYCStatus;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RentalOrderServiceImplTest {

    @Mock
    private RentalOrderRepository rentalOrderRepository;
    @Mock
    private OrderDetailRepository orderDetailRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private DeviceModelRepository deviceModelRepository;
    @Mock
    private PreRentalQcTaskCreator preRentalQcTaskCreator;
    @Mock
    private BookingCalendarService bookingCalendarService;
    @Mock
    private BookingCalendarRepository bookingCalendarRepository;
    @Mock
    private ReservationService reservationService;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private StaffService staffService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private DeviceAllocationQueryService deviceAllocationQueryService;
    @Mock
    private AllocationRepository allocationRepository;
    @Mock
    private AllocationConditionSnapshotRepository allocationConditionSnapshotRepository;
    @Mock
    private DiscrepancyReportRepository discrepancyReportRepository;
    @Mock
    private RentalOrderExtensionRepository rentalOrderExtensionRepository;

    @InjectMocks
    private RentalOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findByIdThrowsForDifferentCustomer() {
        RentalOrder order = baseOrder();
        when(rentalOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(customerRepository.findByAccount_Username("customer"))
                .thenReturn(Optional.of(Customer.builder().customerId(99L).build()));

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("customer", "pass", "ROLE_CUSTOMER")
        );

        assertThatThrownBy(() -> service.findById(1L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test
    void findByIdReturnsResponseForStaff() {
        RentalOrder order = baseOrder();
        when(rentalOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("operator", "pass", "ROLE_OPERATOR")
        );

        var response = service.findById(1L);

        assertThat(response.getOrderId()).isEqualTo(order.getOrderId());
    }

    private RentalOrder baseOrder() {
        Account account = Account.builder()
                .accountId(1L)
                .username("owner")
                .build();
        Customer customer = Customer.builder()
                .customerId(1L)
                .account(account)
                .kycStatus(KYCStatus.VERIFIED)
                .build();
        return RentalOrder.builder()
                .orderId(1L)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(2))
                .orderStatus(OrderStatus.PENDING)
                .customer(customer)
                .depositAmount(BigDecimal.TEN)
                .totalPrice(BigDecimal.valueOf(100))
                .build();
    }
}
