package com.rentaltech.techrental.staff.service.chatservice;

import com.rentaltech.techrental.staff.model.ChatMessage;
import com.rentaltech.techrental.staff.model.ChatMessageSenderType;
import com.rentaltech.techrental.staff.model.Conversation;
import com.rentaltech.techrental.staff.model.dto.ChatMessageCreateRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChatService {
    Conversation getOrCreateConversationByCustomer(Long customerId);
    ChatMessage sendMessage(ChatMessageCreateRequestDto request);
    Page<ChatMessage> getMessages(Long conversationId, Pageable pageable);
    void markMessagesAsRead(Long conversationId, Long currentUserId, ChatMessageSenderType currentUserType);
    Long getUnreadCount(Long conversationId, Long currentUserId, ChatMessageSenderType currentUserType);
    Page<Conversation> getConversationsByStaff(Long staffId, Pageable pageable);
}

