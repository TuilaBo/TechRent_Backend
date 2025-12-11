package com.rentaltech.techrental.config;

import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import com.rentaltech.techrental.contract.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class RentalOrderNotificationScheduler {

    private final RentalOrderRepository rentalOrderRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private static final ScheduledExecutorService EMAIL_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final AtomicLong NEXT_EMAIL_EPOCH_MS = new AtomicLong(0);
    private static final long EMAIL_SPACING_MS = TimeUnit.MINUTES.toMillis(5);

    /**
     * Quét mỗi 12 giờ để thông báo đơn thuê sắp đến hạn (còn dưới 2 ngày).
     */
    @Scheduled(cron = "0 0 */12 * * ?")
    public void notifyNearDueOrders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusDays(1);
        try {
            List<RentalOrder> orders = rentalOrderRepository.findByOrderStatusAndPlanEndDateBetween(
                    OrderStatus.IN_USE, now, threshold);
            orders.forEach(this::notifyCustomerNearDue);
        } catch (Exception ex) {
            log.warn("Không thể gửi thông báo đơn thuê sắp đến hạn: {}", ex.getMessage(), ex);
        }
    }

    private void notifyCustomerNearDue(RentalOrder order) {
        if (order == null || order.getCustomer() == null || order.getCustomer().getCustomerId() == null) {
            return;
        }
        if (rentalOrderRepository.existsByParentOrder(order)) {
            log.debug("Bỏ qua gửi near-due cho đơn {} vì đã có đơn gia hạn", order.getOrderId());
            return;
        }
        if (order.getLastDueNotificationSentAt() != null) {
            log.debug("Bỏ qua gửi near-due cho đơn {} vì đã gửi trước đó vào {}", order.getOrderId(), order.getLastDueNotificationSentAt());
            return;
        }
        String title = "Đơn thuê sắp đến hạn";
        String message = String.format(
                "Đơn thuê #%d sẽ hết hạn vào %s. Vui lòng xác nhận gia hạn hoặc ngày trả hàng.",
                order.getOrderId(),
                order.getPlanEndDate() != null ? order.getPlanEndDate() : "thời gian đã đặt"
        );
        try {
            notificationService.notifyAccount(
                    order.getCustomer().getAccount().getAccountId(),
                    NotificationType.ORDER_NEAR_DUE,
                    title,
                    message
            );
            sendEmailAsync(order.getCustomer().getEmail(), title, message);
            order.setLastDueNotificationSentAt(LocalDateTime.now());
            rentalOrderRepository.save(order);
        } catch (Exception ex) {
            log.warn("Không thể gửi thông báo sắp đến hạn cho đơn {}: {}", order.getOrderId(), ex.getMessage());
        }
    }

    /**
     * Fire-and-forget email; errors are logged but do not stop scheduler.
     */
    private void sendEmailAsync(String email, String subject, String body) {
        if (email == null || email.isBlank()) {
            return;
        }
        try {
            long now = System.currentTimeMillis();
            long scheduledAt = Math.max(now, NEXT_EMAIL_EPOCH_MS.get());
            long delay = scheduledAt - now;
            NEXT_EMAIL_EPOCH_MS.set(scheduledAt + EMAIL_SPACING_MS);

            EMAIL_EXECUTOR.schedule(() -> {
                try {
                    emailService.sendGeneric(email, subject, body);
                } catch (Exception e) {
                    log.warn("Không thể gửi email nhắc hạn tới {}: {}", email, e.getMessage());
                }
            }, delay, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("Không thể khởi tạo gửi email async tới {}: {}", email, e.getMessage());
        }
    }
}
