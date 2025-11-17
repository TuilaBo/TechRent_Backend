package com.rentaltech.techrental.staff.service.settlementservice;

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

import java.time.LocalDateTime;
import java.util.Comparator;
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

    private static final String STAFF_NOTIFICATION_TOPIC_TEMPLATE = "/topic/staffs/%d/notifications";

    @Override
    public Settlement create(SettlementCreateRequestDto request) {
        RentalOrder order = rentalOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy đơn thuê: " + request.getOrderId()));

        Settlement settlement = Settlement.builder()
                .rentalOrder(order)
                .totalRent(request.getTotalRent())
                .damageFee(request.getDamageFee())
                .lateFee(request.getLateFee())
                .accessoryFee(request.getAccessoryFee())
                .depositUsed(request.getDepositUsed())
                .finalAmount(request.getFinalAmount())
                .state(SettlementState.Draft)
                .issuedAt(null)
                .build();

        Settlement saved = settlementRepository.save(settlement);
        notifyCustomerDepositIssued(order);
        return saved;
    }

    @Override
    public Settlement update(Long settlementId, SettlementUpdateRequestDto request) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy settlement: " + settlementId));

        if (request.getTotalRent() != null) settlement.setTotalRent(request.getTotalRent());
        if (request.getDamageFee() != null) settlement.setDamageFee(request.getDamageFee());
        if (request.getLateFee() != null) settlement.setLateFee(request.getLateFee());
        if (request.getAccessoryFee() != null) settlement.setAccessoryFee(request.getAccessoryFee());
        if (request.getDepositUsed() != null) settlement.setDepositUsed(request.getDepositUsed());
        if (request.getFinalAmount() != null) settlement.setFinalAmount(request.getFinalAmount());
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

    private void notifyCustomerDepositIssued(RentalOrder order) {
        if (order == null || order.getCustomer() == null || order.getCustomer().getCustomerId() == null) {
            return;
        }
        notificationService.notifyCustomer(
                order.getCustomer().getCustomerId(),
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
                "Khách đã xác nhận hoàn cọc cho đơn #" + orderId + ". Vui lòng hỗ trợ hoàn tất thu hồi."
        );
        latestTask.getAssignedStaff().stream()
                .filter(Objects::nonNull)
                .filter(staff -> staff.getStaffRole() == StaffRole.CUSTOMER_SUPPORT_STAFF)
                .map(Staff::getStaffId)
                .filter(Objects::nonNull)
                .forEach(staffId -> {
                    String destination = String.format(STAFF_NOTIFICATION_TOPIC_TEMPLATE, staffId);
                    try {
                        messagingTemplate.convertAndSend(destination, payload);
                    } catch (Exception ex) {
                        // only log locally; do not interrupt flow
                    }
                });
    }

    private record SettlementSupportNotification(Long taskId, Long orderId, String message) {}
}

