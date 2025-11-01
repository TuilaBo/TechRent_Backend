package com.rentaltech.techrental.staff.repository;

import com.rentaltech.techrental.staff.model.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByDispute_DisputeId(Long disputeId);
    Optional<Conversation> findByCustomer_CustomerIdAndStaff_StaffId(Long customerId, Long staffId);
    Optional<Conversation> findByCustomer_CustomerId(Long customerId);
    List<Conversation> findByStaff_StaffId(Long staffId);
    Page<Conversation> findByStaff_StaffId(Long staffId, Pageable pageable);
}

