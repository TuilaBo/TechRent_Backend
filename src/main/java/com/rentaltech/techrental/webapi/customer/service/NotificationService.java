package com.rentaltech.techrental.webapi.customer.service;

import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.model.dto.NotificationResponseDto;

import java.util.List;

public interface NotificationService {

    NotificationResponseDto notifyCustomer(Long customerId,
                                           NotificationType type,
                                           String title,
                                           String message);

    List<NotificationResponseDto> getNotificationsForCustomer(Long customerId);
}
