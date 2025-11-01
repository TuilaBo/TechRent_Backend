package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findByConversation_ConversationIdOrderBySentAtDesc(Long conversationId, Pageable pageable);
    List<ChatMessage> findByConversation_ConversationIdAndIsReadFalse(Long conversationId);
    Long countByConversation_ConversationIdAndIsReadFalse(Long conversationId);
}

