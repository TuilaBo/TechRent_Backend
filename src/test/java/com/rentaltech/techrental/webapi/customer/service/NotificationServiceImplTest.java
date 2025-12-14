package com.rentaltech.techrental.webapi.customer.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.repository.AccountRepository;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.Notification;
import com.rentaltech.techrental.webapi.customer.model.NotificationType;
import com.rentaltech.techrental.webapi.customer.model.dto.NotificationResponseDto;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import com.rentaltech.techrental.webapi.customer.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private FirebaseNotificationService firebaseNotificationService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void notifyAccountPersistsBroadcastsAndSendsFcmWhenTokenPresent() {
        Account account = Account.builder()
                .accountId(1L)
                .username("u")
                .email("u@example.com")
                .password("secret")
                .build();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(10L);
            return notification;
        });
        when(customerRepository.findByAccount_AccountId(1L)).thenReturn(Optional.of(
                Customer.builder().fcmToken("token").build()
        ));

        NotificationResponseDto response = notificationService.notifyAccount(
                1L, NotificationType.ORDER_CONFIRMED, "title", "message"
        );

        assertThat(response.getNotificationId()).isEqualTo(10L);
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getAccount()).isEqualTo(account);

        verify(messagingTemplate).convertAndSend(eq("/topic/accounts/1/notifications"), any(NotificationResponseDto.class));
        verify(firebaseNotificationService).sendNotification("token", "title", "message", NotificationType.ORDER_CONFIRMED);
    }

    @Test
    void notifyAccountSkipsFcmWhenCustomerMissing() {
        Account account = Account.builder()
                .accountId(2L)
                .username("u2")
                .email("u2@example.com")
                .password("secret")
                .build();
        when(accountRepository.findById(2L)).thenReturn(Optional.of(account));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerRepository.findByAccount_AccountId(2L)).thenReturn(Optional.empty());

        notificationService.notifyAccount(2L, NotificationType.ORDER_NEAR_DUE, "title", "body");

        verify(firebaseNotificationService, never()).sendNotification(any(), any(), any(), any());
    }

    @Test
    void getNotificationsForAccountReturnsDtosOrdered() {
        Notification notification = Notification.builder()
                .notificationId(5L)
                .title("t")
                .build();
        when(notificationRepository.findByAccount_AccountIdOrderByCreatedAtDesc(9L))
                .thenReturn(List.of(notification));

        List<NotificationResponseDto> dtos = notificationService.getNotificationsForAccount(9L);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getNotificationId()).isEqualTo(5L);
    }
}
