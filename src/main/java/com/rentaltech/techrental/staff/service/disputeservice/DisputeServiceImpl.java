/* Dispute feature temporarily disabled
package com.rentaltech.techrental.staff.service.disputeservice;

import com.rentaltech.techrental.staff.model.*;
import com.rentaltech.techrental.staff.model.dto.DisputeCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.DisputeUpdateRequestDto;
import com.rentaltech.techrental.staff.repository.DisputeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class DisputeServiceImpl implements DisputeService {

    private final DisputeRepository disputeRepository;

    @Override
    public Dispute create(DisputeCreateRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        if (request.getOpenBy() == DisputeOpenBy.CUSTOMER) {
            if (request.getCustomerId() == null) {
                throw new IllegalArgumentException("customerId is required when openBy = CUSTOMER");
            }
        } else {
            if (request.getStaffId() == null) {
                throw new IllegalArgumentException("staffId is required when openBy = STAFF");
            }
        }

        Dispute dispute = Dispute.builder()
                .settlement(Settlement.builder().settlementId(request.getSettlementId()).build())
                .openBy(request.getOpenBy())
                .openedByCustomer(request.getCustomerId() == null ? null : Customer.builder().customerId(request.getCustomerId()).build())
                .openedByStaff(request.getStaffId() == null ? null : Staff.builder().staffId(request.getStaffId()).build())
                .reason(request.getReason())
                .detail(request.getDetail())
                .status(DisputeStatus.Open)
                .openAt(LocalDateTime.now())
                .build();

        return disputeRepository.save(dispute);
    }

    @Override
    public Dispute update(Long disputeId, DisputeUpdateRequestDto request) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new NoSuchElementException("Dispute not found: " + disputeId));

        if (request.getReason() != null) dispute.setReason(request.getReason());
        if (request.getDetail() != null) dispute.setDetail(request.getDetail());
        if (request.getStatus() != null) {
            dispute.setStatus(request.getStatus());
            if (request.getStatus() == DisputeStatus.Closed && dispute.getClosedAt() == null) {
                dispute.setClosedAt(LocalDateTime.now());
            }
        }

        return disputeRepository.save(dispute);
    }

    @Override
    public Dispute getBySettlementId(Long settlementId) {
        return disputeRepository.findBySettlement_SettlementId(settlementId)
                .orElseThrow(() -> new NoSuchElementException("Dispute not found for settlementId: " + settlementId));
    }
}
*/
