package com.rentaltech.techrental.webapi.customer.model.dto;

import com.rentaltech.techrental.webapi.customer.model.Notification;
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponseDto {
    private final Long notificationId;
    private final Long accountId;
    private final String title;
    private final String message;
    private final NotificationType type;
    private final boolean read;
    private final LocalDateTime createdAt;

    public static NotificationResponseDto from(Notification notification) {
        if (notification == null) {
            return null;
        }
        var account = notification.getAccount();
        return NotificationResponseDto.builder()
                .notificationId(notification.getNotificationId())
                .accountId(account != null ? account.getAccountId() : null)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .read(Boolean.TRUE.equals(notification.getRead()))
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
