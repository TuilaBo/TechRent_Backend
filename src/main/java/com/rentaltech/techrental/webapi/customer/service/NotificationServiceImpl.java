package com.rentaltech.techrental.webapi.customer.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.repository.AccountRepository;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.Notification;
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.model.dto.NotificationResponseDto;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import com.rentaltech.techrental.webapi.customer.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final FirebaseNotificationService firebaseNotificationService;

    private static final String TOPIC_TEMPLATE = "/topic/accounts/%d/notifications";

    @Override
    @Transactional
    public NotificationResponseDto notifyAccount(Long accountId,
                                                 NotificationType type,
                                                 String title,
                                                 String message) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy account với id: " + accountId));

        Notification notification = Notification.builder()
                .account(account)
                .title(title)
                .message(message)
                .type(type)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationResponseDto dto = toDto(saved);

        // WebSocket broadcast
        messagingTemplate.convertAndSend(String.format(TOPIC_TEMPLATE, accountId), dto);

        // Push via FCM nếu là customer và có token
        customerRepository.findByAccount_AccountId(accountId)
                .map(Customer::getFcmToken)
                .filter(StringUtils::hasText)
                .ifPresent(token -> firebaseNotificationService.sendNotification(token, title, message, type));

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotificationsForAccount(Long accountId) {
        return notificationRepository.findByAccount_AccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private NotificationResponseDto toDto(Notification notification) {
        return NotificationResponseDto.builder()
                .notificationId(notification.getNotificationId())
                .accountId(notification.getAccount().getAccountId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .read(Boolean.TRUE.equals(notification.getRead()))
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
