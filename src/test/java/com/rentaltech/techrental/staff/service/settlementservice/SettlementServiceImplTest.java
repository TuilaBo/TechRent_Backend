package com.rentaltech.techrental.staff.service.settlementservice;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.device.model.DiscrepancyReport;
import com.rentaltech.techrental.device.repository.DiscrepancyReportRepository;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.staff.model.*;
import com.rentaltech.techrental.staff.model.dto.SettlementCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.SettlementUpdateRequestDto;
import com.rentaltech.techrental.staff.repository.SettlementRepository;
import com.rentaltech.techrental.staff.repository.TaskCategoryRepository;
import com.rentaltech.techrental.staff.repository.TaskRepository;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceImplTest {

    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private RentalOrderRepository rentalOrderRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskCategoryRepository taskCategoryRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private DiscrepancyReportRepository discrepancyReportRepository;

    @InjectMocks
    private SettlementServiceImpl settlementService;

    @Test
    void createSettlementPersistsDraftAndNotifiesCustomer() {
        SettlementCreateRequestDto request = SettlementCreateRequestDto.builder()
                .orderId(1L)
                .totalDeposit(BigDecimal.valueOf(500))
                .damageFee(BigDecimal.ZERO)
                .lateFee(BigDecimal.ZERO)
                .finalReturnAmount(BigDecimal.valueOf(500))
                .build();

        RentalOrder order = rentalOrder(1L, 11L, 111L);
        when(rentalOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Settlement saved = settlementService.create(request);

        assertThat(saved.getState()).isEqualTo(SettlementState.Draft);
        assertThat(saved.getIssuedAt()).isNull();
        assertThat(saved.getRentalOrder()).isEqualTo(order);

        verify(notificationService).notifyAccount(eq(111L), eq(NotificationType.ORDER_CONFIRMED), anyString(), anyString());
        verify(settlementRepository).save(saved);
    }

    @Test
    void createAutomaticForOrderUpdatesExistingSettlement() {
        long orderId = 2L;
        RentalOrder order = rentalOrder(orderId, 22L, 222L);
        Settlement existing = Settlement.builder()
                .settlementId(5L)
                .rentalOrder(order)
                .build();

        when(rentalOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(settlementRepository.findByRentalOrder_OrderId(orderId)).thenReturn(Optional.of(existing));
        when(settlementRepository.save(existing)).thenReturn(existing);
        when(discrepancyReportRepository.findByAllocation_OrderDetail_RentalOrder_OrderId(orderId))
                .thenReturn(List.of(
                        DiscrepancyReport.builder().penaltyAmount(BigDecimal.valueOf(50)).build(),
                        DiscrepancyReport.builder().penaltyAmount(BigDecimal.valueOf(25)).build()
                ));

        Settlement updated = settlementService.createAutomaticForOrder(orderId);

        assertThat(updated.getTotalDeposit()).isEqualTo(order.getDepositAmount());
        assertThat(updated.getDamageFee()).isEqualTo(BigDecimal.valueOf(75));
        assertThat(updated.getFinalReturnAmount()).isEqualTo(order.getDepositAmount().subtract(BigDecimal.valueOf(75)));
        verify(settlementRepository).save(existing);
    }

    @Test
    void createAutomaticForOrderCreatesNewSettlementWhenMissing() {
        long orderId = 3L;
        RentalOrder order = rentalOrder(orderId, 33L, 333L);

        when(rentalOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(settlementRepository.findByRentalOrder_OrderId(orderId)).thenReturn(Optional.empty());
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(discrepancyReportRepository.findByAllocation_OrderDetail_RentalOrder_OrderId(orderId))
                .thenReturn(List.of());

        Settlement created = settlementService.createAutomaticForOrder(orderId);

        assertThat(created.getTotalDeposit()).isEqualTo(order.getDepositAmount());
        assertThat(created.getState()).isEqualTo(SettlementState.Draft);
        verify(settlementRepository).save(created);
    }

    @Test
    void updateAppliesIssuedTimestampWhenStateBecomesIssued() {
        Settlement settlement = Settlement.builder()
                .settlementId(7L)
                .state(SettlementState.Draft)
                .build();

        SettlementUpdateRequestDto request = SettlementUpdateRequestDto.builder()
                .state(SettlementState.Issued)
                .damageFee(BigDecimal.valueOf(10))
                .build();

        when(settlementRepository.findById(7L)).thenReturn(Optional.of(settlement));
        when(settlementRepository.save(settlement)).thenReturn(settlement);

        Settlement updated = settlementService.update(7L, request);

        assertThat(updated.getState()).isEqualTo(SettlementState.Issued);
        assertThat(updated.getDamageFee()).isEqualTo(BigDecimal.valueOf(10));
        assertThat(updated.getIssuedAt()).isNotNull();
    }

    @Test
    void respondToSettlementValidatesOwnership() {
        Settlement settlement = Settlement.builder()
                .settlementId(9L)
                .rentalOrder(rentalOrder(44L, 55L, 555L, "owner"))
                .state(SettlementState.Draft)
                .build();
        when(settlementRepository.findById(9L)).thenReturn(Optional.of(settlement));

        assertThatThrownBy(() -> settlementService.respondToSettlement(9L, true, "intruder"))
                .isInstanceOf(AccessDeniedException.class);
        verify(settlementRepository, never()).save(any());
    }

    @Test
    void respondToSettlementAcceptedSendsSupportNotifications() {
        String username = "owner";
        long orderId = 66L;
        Staff supportStaff = Staff.builder()
                .staffId(101L)
                .staffRole(StaffRole.CUSTOMER_SUPPORT_STAFF)
                .account(Account.builder().accountId(202L).username("support").email("support@example.com").password("secret").role(null).build())
                .build();
        TaskCategory pickupCategory = TaskCategory.builder()
                .taskCategoryId(15L)
                .name("Pick up rental order")
                .build();
        Task task = Task.builder()
                .taskId(301L)
                .taskCategory(pickupCategory)
                .orderId(orderId)
                .assignedStaff(new LinkedHashSet<>(Set.of(supportStaff)))
                .createdAt(LocalDateTime.now().minusHours(2))
                .status(TaskStatus.PENDING)
                .build();

        Settlement settlement = Settlement.builder()
                .settlementId(12L)
                .state(SettlementState.Draft)
                .rentalOrder(rentalOrder(orderId, 77L, 777L, username))
                .build();

        when(settlementRepository.findById(12L)).thenReturn(Optional.of(settlement));
        when(taskCategoryRepository.findByNameIgnoreCase("Pick up rental order")).thenReturn(Optional.of(pickupCategory));
        when(taskRepository.findByOrderId(orderId)).thenReturn(List.of(task));
        when(settlementRepository.save(settlement)).thenReturn(settlement);

        Settlement result = settlementService.respondToSettlement(12L, true, username);

        assertThat(result.getState()).isEqualTo(SettlementState.Issued);
        assertThat(result.getIssuedAt()).isNotNull();
        verify(notificationService).notifyAccount(eq(202L), eq(NotificationType.ORDER_NEAR_DUE), anyString(), anyString());
        verify(messagingTemplate).convertAndSend(eq("/topic/staffs/101/notifications"), any(Object.class));
    }

    private RentalOrder rentalOrder(Long orderId, Long customerId, Long accountId) {
        return rentalOrder(orderId, customerId, accountId, "customer-" + accountId);
    }

    private RentalOrder rentalOrder(Long orderId, Long customerId, Long accountId, String username) {
        return RentalOrder.builder()
                .orderId(orderId)
                .depositAmount(BigDecimal.valueOf(500))
                .customer(Customer.builder()
                        .customerId(customerId)
                        .account(Account.builder()
                                .accountId(accountId)
                                .username(username)
                                .email(username + "@example.com")
                                .password("secret")
                                .role(null)
                                .build())
                        .build())
                .build();
    }
}
