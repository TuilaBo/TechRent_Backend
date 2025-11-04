package com.rentaltech.techrental.staff.service.disputeservice;

import com.rentaltech.techrental.staff.model.*;
import com.rentaltech.techrental.staff.model.dto.DisputeCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.DisputeUpdateRequestDto;
import com.rentaltech.techrental.staff.repository.DisputeRepository;
import com.rentaltech.techrental.staff.repository.SettlementRepository;
import com.rentaltech.techrental.staff.repository.StaffRepository;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class DisputeServiceImpl implements DisputeService {

    private final DisputeRepository disputeRepository;
    private final SettlementRepository settlementRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;

    @Override
    public Dispute create(DisputeCreateRequestDto request) {
        Settlement settlement = settlementRepository.findById(request.getSettlementId())
                .orElseThrow(() -> new NoSuchElementException("Settlement not found: " + request.getSettlementId()));

        Customer customer = null;
        Staff staff = null;

        if (request.getOpenBy() == DisputeOpenBy.CUSTOMER) {
            if (request.getCustomerId() == null) {
                throw new IllegalArgumentException("customerId is required when openBy = CUSTOMER");
            }
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new NoSuchElementException("Customer not found: " + request.getCustomerId()));
        } else {
            if (request.getStaffId() == null) {
                throw new IllegalArgumentException("staffId is required when openBy is not CUSTOMER");
            }
            staff = staffRepository.findById(request.getStaffId())
                    .orElseThrow(() -> new NoSuchElementException("Staff not found: " + request.getStaffId()));
        }

        Dispute dispute = Dispute.builder()
                .settlement(settlement)
                .openBy(request.getOpenBy())
                .openedByCustomer(customer)
                .openedByStaff(staff)
                .reason(request.getReason())
                .detail(request.getDetail())
                .status(DisputeStatus.Open)
                .openAt(LocalDateTime.now())
                .closedAt(null)
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
    @Transactional(readOnly = true)
    public Dispute getBySettlementId(Long settlementId) {
        return disputeRepository.findBySettlement_SettlementId(settlementId)
                .orElseThrow(() -> new NoSuchElementException("Dispute not found for settlementId: " + settlementId));
    }
}

