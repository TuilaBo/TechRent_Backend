package com.rentaltech.techrental.staff.service.settlementservice;

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
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class SettlementServiceImpl implements SettlementService {

    private final SettlementRepository settlementRepository;
    private final RentalOrderRepository rentalOrderRepository;
    private final TaskRepository taskRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DiscrepancyReportRepository discrepancyReportRepository;

    private static final String STAFF_NOTIFICATION_TOPIC_TEMPLATE = "/topic/staffs/%d/notifications";
    private static final BigDecimal LATE_FEE_RATE = new BigDecimal("0.10");

    @Override
    public Settlement create(SettlementCreateRequestDto request) {
        RentalOrder order = rentalOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + request.getOrderId()));

        Settlement settlement = Settlement.builder()
                .rentalOrder(order)
                .totalDeposit(request.getTotalDeposit())
                .damageFee(request.getDamageFee())
                .lateFee(request.getLateFee())
                .accessoryFee(request.getAccessoryFee())
                .finalReturnAmount(request.getFinalReturnAmount())
                .state(SettlementState.Draft)
                .issuedAt(null)
                .build();

        Settlement saved = settlementRepository.save(settlement);
        notifyCustomerSettlementCreated(order);
        return saved;
    }

    @Override
    public Settlement createAutomaticForOrder(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
        RentalOrder order = rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + orderId));
        BigDecimal totalDeposit = order.getDepositAmount() != null ? order.getDepositAmount() : BigDecimal.ZERO;
        BigDecimal damageFee = calculateFinalDiscrepancyDamageFee(order);
        BigDecimal lateFee = calculateLateFee(order);
        BigDecimal accessoryFee = BigDecimal.ZERO;
        BigDecimal finalReturn = totalDeposit
                .subtract(damageFee)
                .subtract(lateFee)
                .subtract(accessoryFee);
        return settlementRepository.findByRentalOrder_OrderId(orderId)
                .map(existing -> {
                    existing.setTotalDeposit(totalDeposit);
                    existing.setDamageFee(damageFee);
                    existing.setLateFee(lateFee);
                    existing.setAccessoryFee(accessoryFee);
                    existing.setFinalReturnAmount(finalReturn);
                    return settlementRepository.save(existing);
                })
                .orElseGet(() -> {
                    SettlementCreateRequestDto request = SettlementCreateRequestDto.builder()
                            .orderId(orderId)
                            .totalDeposit(totalDeposit)
                            .damageFee(damageFee)
                            .lateFee(lateFee)
                            .accessoryFee(accessoryFee)
                            .finalReturnAmount(finalReturn)
                            .build();
                    return create(request);
                });
    }

    @Override
    public Settlement update(Long settlementId, SettlementUpdateRequestDto request) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy settlement: " + settlementId));

        if (request.getTotalDeposit() != null) settlement.setTotalDeposit(request.getTotalDeposit());
        if (request.getDamageFee() != null) settlement.setDamageFee(request.getDamageFee());
        if (request.getLateFee() != null) settlement.setLateFee(request.getLateFee());
        if (request.getAccessoryFee() != null) settlement.setAccessoryFee(request.getAccessoryFee());
        if (request.getFinalReturnAmount() != null) settlement.setFinalReturnAmount(request.getFinalReturnAmount());
        if (request.getState() != null) {
            settlement.setState(request.getState());
            if (request.getState() == SettlementState.Issued && settlement.getIssuedAt() == null) {
                settlement.setIssuedAt(LocalDateTime.now());
            }
        }

        return settlementRepository.save(settlement);
    }

    @Override
    @Transactional(readOnly = true)
    public Settlement getByOrderId(Long orderId) {
        return settlementRepository.findByRentalOrder_OrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy settlement cho đơn thuê: " + orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Settlement> getAll(Pageable pageable) {
        return settlementRepository.findAll(pageable);
    }

    @Override
    public Settlement respondToSettlement(Long settlementId, boolean accepted, String username) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy settlement: " + settlementId));
        RentalOrder order = settlement.getRentalOrder();
        if (order == null || order.getCustomer() == null || order.getCustomer().getAccount() == null) {
            throw new AccessDeniedException("Không xác định được chủ sở hữu settlement");
        }
        if (!order.getCustomer().getAccount().getUsername().equals(username)) {
            throw new AccessDeniedException("Không có quyền phản hồi settlement này");
        }

        if (accepted) {
            settlement.setState(SettlementState.Issued);
            if (settlement.getIssuedAt() == null) {
                settlement.setIssuedAt(LocalDateTime.now());
            }
            notifySupportStaffPickup(order.getOrderId());
        } else {
            settlement.setState(SettlementState.Draft);
        }
        return settlementRepository.save(settlement);
    }

    private void notifyCustomerSettlementCreated(RentalOrder order) {
        if (order == null || order.getCustomer() == null || order.getCustomer().getCustomerId() == null) {
            return;
        }
        notificationService.notifyAccount(
                order.getCustomer().getAccount().getAccountId(),
                NotificationType.ORDER_CONFIRMED,
                "Hóa đơn hoàn cọc đã được tạo",
                "Hóa đơn hoàn cọc cho đơn hàng #" + order.getOrderId() + " đã được tạo"
        );
    }

    private void notifySupportStaffPickup(Long orderId) {
        if (orderId == null) {
            return;
        }
        TaskCategory pickupCategory = taskCategoryRepository.findByNameIgnoreCase("Pick up rental order")
                .orElseGet(() -> taskCategoryRepository.findByName("Pick up rental order").orElse(null));
        if (pickupCategory == null) {
            return;
        }
        Task latestTask = taskRepository.findByOrderId(orderId).stream()
                .filter(task -> task.getTaskCategory() != null
                        && pickupCategory.getTaskCategoryId().equals(task.getTaskCategory().getTaskCategoryId()))
                .max(Comparator.comparing(Task::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)))
                .orElse(null);
        if (latestTask == null || latestTask.getAssignedStaff() == null) {
            return;
        }
        SettlementSupportNotification payload = new SettlementSupportNotification(
                latestTask.getTaskId(),
                orderId,
                "Khách chấp nhận hoàn cọc cho đơn #" + orderId + ". Vui lòng hỗ trợ hoàn tất thu hồi."
        );
        latestTask.getAssignedStaff().stream()
                .filter(Objects::nonNull)
                .filter(staff -> staff.getStaffRole() == StaffRole.CUSTOMER_SUPPORT_STAFF)
                .forEach(staff -> {
                    if (staff.getAccount() != null && staff.getAccount().getAccountId() != null) {
                        notificationService.notifyAccount(
                                staff.getAccount().getAccountId(),
                                NotificationType.ORDER_NEAR_DUE,
                                "Khách xác nhận hoàn cọc",
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
                        // only log locally; do not interrupt flow
                    }
                });
    }

    private record SettlementSupportNotification(Long taskId, Long orderId, String message) {}

    private BigDecimal calculateFinalDiscrepancyDamageFee(RentalOrder order) {
        if (order == null || order.getOrderId() == null) {
            return BigDecimal.ZERO;
        }
        List<DiscrepancyReport> reports = discrepancyReportRepository.findByAllocation_OrderDetail_RentalOrder_OrderId(order.getOrderId());
        if (reports == null || reports.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return reports.stream()
                .map(report -> report.getPenaltyAmount() != null ? report.getPenaltyAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateLateFee(RentalOrder order) {
        if (order == null || order.getPlanEndDate() == null || order.getEndDate() == null || order.getPricePerDay() == null) {
            return BigDecimal.ZERO;
        }
        LocalDateTime threshold = order.getPlanEndDate().plusHours(1);
        LocalDateTime returnedAt = order.getEndDate();
        if (!returnedAt.isAfter(threshold)) {
            return BigDecimal.ZERO;
        }
        long hoursLate = ChronoUnit.HOURS.between(threshold, returnedAt);
        if (hoursLate <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal hourlyRate = order.getPricePerDay().multiply(LATE_FEE_RATE);
        return hourlyRate.multiply(BigDecimal.valueOf(hoursLate));
    }
}
