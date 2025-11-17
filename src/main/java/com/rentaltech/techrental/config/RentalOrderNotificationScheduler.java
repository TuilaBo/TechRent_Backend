package com.rentaltech.techrental.config;

import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RentalOrderNotificationScheduler {

    private final RentalOrderRepository rentalOrderRepository;
    private final NotificationService notificationService;

    /**
     * Quét mỗi 12 giờ để thông báo đơn thuê sắp đến hạn (còn dưới 2 ngày).
     */
    @Scheduled(cron = "0 0 */12 * * ?")
    public void notifyNearDueOrders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusDays(2);
        try {
            List<RentalOrder> orders = rentalOrderRepository.findByOrderStatusAndEndDateBetween(
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
        String title = "Đơn thuê sắp đến hạn";
        String message = String.format(
                "Đơn thuê #%d sẽ hết hạn vào %s. Vui lòng xác nhận gia hạn hoặc ngày trả hàng.",
                order.getOrderId(),
                order.getEndDate() != null ? order.getEndDate() : "thời gian đã đặt"
        );
        try {
            notificationService.notifyCustomer(
                    order.getCustomer().getCustomerId(),
                    NotificationType.ORDER_NEAR_DUE,
                    title,
                    message
            );
        } catch (Exception ex) {
            log.warn("Không thể gửi thông báo sắp đến hạn cho đơn {}: {}", order.getOrderId(), ex.getMessage());
        }
    }
}
