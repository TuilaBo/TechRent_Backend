package com.rentaltech.techrental.staff.service.chatservice;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.staff.model.*;
import com.rentaltech.techrental.staff.model.dto.ChatMessageCreateRequestDto;
import com.rentaltech.techrental.staff.repository.ChatMessageRepository;
import com.rentaltech.techrental.staff.repository.ConversationRepository;
import com.rentaltech.techrental.staff.repository.StaffRepository;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Test
    void getOrCreateConversationReturnsExistingConversation() {
        Conversation conversation = Conversation.builder().conversationId(5L).build();
        when(conversationRepository.findByCustomer_CustomerId(1L)).thenReturn(Optional.of(conversation));

        Conversation result = chatService.getOrCreateConversationByCustomer(1L);

        assertThat(result).isEqualTo(conversation);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void getOrCreateConversationCreatesNewWithFallbackStaff() {
        Customer customer = Customer.builder()
                .customerId(2L)
                .account(Account.builder().accountId(20L).username("cust").email("cust@example.com").password("secret").role(null).build())
                .build();
        Staff operator = Staff.builder()
                .staffId(3L)
                .staffRole(StaffRole.OPERATOR)
                .account(Account.builder().accountId(30L).username("op").email("op@example.com").password("secret").role(null).build())
                .build();

        when(conversationRepository.findByCustomer_CustomerId(2L)).thenReturn(Optional.empty());
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(staffRepository.findByStaffRoleAndIsActiveTrue(StaffRole.CUSTOMER_SUPPORT_STAFF)).thenReturn(Collections.emptyList());
        when(staffRepository.findByStaffRoleAndIsActiveTrue(StaffRole.ADMIN)).thenReturn(Collections.emptyList());
        when(staffRepository.findByStaffRoleAndIsActiveTrue(StaffRole.OPERATOR)).thenReturn(List.of(operator));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation saved = invocation.getArgument(0);
            saved.setConversationId(10L);
            return saved;
        });

        Conversation created = chatService.getOrCreateConversationByCustomer(2L);

        assertThat(created.getConversationId()).isEqualTo(10L);
        assertThat(created.getStaff()).isEqualTo(operator);
        assertThat(created.getCustomer()).isEqualTo(customer);
    }

    @Test
    void sendMessagePersistsMessageWithDefaults() {
        Conversation conversation = Conversation.builder().conversationId(9L).build();
        ChatMessageCreateRequestDto request = ChatMessageCreateRequestDto.builder()
                .conversationId(9L)
                .senderId(7L)
                .senderType(ChatMessageSenderType.CUSTOMER)
                .content("hello")
                .build();

        when(conversationRepository.findById(9L)).thenReturn(Optional.of(conversation));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage message = chatService.sendMessage(request);

        assertThat(message.getConversation()).isEqualTo(conversation);
        assertThat(message.getIsRead()).isFalse();
        assertThat(message.getSentAt()).isNotNull();
    }

    @Test
    void markMessagesAsReadSkipsCurrentUserMessages() {
        ChatMessage ownMessage = ChatMessage.builder()
                .messageId(1L)
                .senderId(11L)
                .senderType(ChatMessageSenderType.STAFF)
                .isRead(false)
                .build();
        ChatMessage otherMessage = ChatMessage.builder()
                .messageId(2L)
                .senderId(99L)
                .senderType(ChatMessageSenderType.CUSTOMER)
                .isRead(false)
                .build();

        when(chatMessageRepository.findByConversation_ConversationIdAndIsReadFalse(4L))
                .thenReturn(List.of(ownMessage, otherMessage));
        when(chatMessageRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        chatService.markMessagesAsRead(4L, 11L, ChatMessageSenderType.STAFF);

        assertThat(ownMessage.getIsRead()).isFalse();
        assertThat(otherMessage.getIsRead()).isTrue();
        assertThat(otherMessage.getReadAt()).isNotNull();
        verify(chatMessageRepository).saveAll(any());
    }

    @Test
    void getUnreadCountFiltersBySender() {
        ChatMessage messageFromCurrent = ChatMessage.builder()
                .senderId(5L)
                .senderType(ChatMessageSenderType.CUSTOMER)
                .isRead(false)
                .build();
        ChatMessage messageFromOther = ChatMessage.builder()
                .senderId(6L)
                .senderType(ChatMessageSenderType.STAFF)
                .isRead(false)
                .build();

        when(chatMessageRepository.findByConversation_ConversationIdAndIsReadFalse(6L))
                .thenReturn(List.of(messageFromCurrent, messageFromOther));

        long unread = chatService.getUnreadCount(6L, 5L, ChatMessageSenderType.CUSTOMER);

        assertThat(unread).isEqualTo(1L);
    }
}
