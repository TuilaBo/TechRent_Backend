package com.rentaltech.techrental.webapi.customer.model.dto;

import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponseDto {
    private final Long notificationId;
    private final Long customerId;
    private final String title;
    private final String message;
    private final NotificationType type;
    private final boolean read;
    private final LocalDateTime createdAt;
}
