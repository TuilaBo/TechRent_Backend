package com.rentaltech.techrental.webapi.customer.controller;

import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.model.dto.NotificationResponseDto;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * Bridges STOMP messages from clients into the existing notification service so that
 * WebSocket clients can trigger notifications and receive broadcasts on their topics.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketController {

    private final NotificationService notificationService;

    @MessageMapping("/notifications.send")
    public void handleNotification(@Payload NotificationMessagePayload payload) {
        log.info("Received notification payload over WebSocket: {}", payload);
        NotificationResponseDto response = notificationService.notifyAccount(
                payload.getAccountId(),
                payload.getType(),
                payload.getTitle(),
                payload.getMessage()
        );
        log.debug("Notification persisted and dispatched with id {}", response.getNotificationId());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationMessagePayload {
        private Long accountId;
        private NotificationType type;
        private String title;
        private String message;
    }
}
