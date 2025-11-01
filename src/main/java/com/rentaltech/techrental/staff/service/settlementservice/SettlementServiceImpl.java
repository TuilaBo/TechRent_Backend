package com.rentaltech.techrental.staff.service.settlementservice;

import com.rentaltech.techrental.staff.model.Settlement;
import com.rentaltech.techrental.staff.model.SettlementState;
import com.rentaltech.techrental.staff.model.dto.SettlementCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.SettlementUpdateRequestDto;
import com.rentaltech.techrental.staff.repository.SettlementRepository;
import com.rentaltech.techrental.webapi.customer.model.RentalOrder;
import com.rentaltech.techrental.webapi.customer.repository.RentalOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class SettlementServiceImpl implements SettlementService {

    private final SettlementRepository settlementRepository;
    private final RentalOrderRepository rentalOrderRepository;

    @Override
    public Settlement create(SettlementCreateRequestDto request) {
        RentalOrder order = rentalOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new NoSuchElementException("RentalOrder not found: " + request.getOrderId()));

        Settlement settlement = Settlement.builder()
                .rentalOrder(order)
                .totalRent(request.getTotalRent())
                .damageFee(request.getDamageFee())
                .lateFee(request.getLateFee())
                .accessoryFee(request.getAccessoryFee())
                .depositUsed(request.getDepositUsed())
                .finalAmount(request.getFinalAmount())
                .state(SettlementState.Draft)
                .issuedAt(null)
                .build();

        return settlementRepository.save(settlement);
    }

    @Override
    public Settlement update(Long settlementId, SettlementUpdateRequestDto request) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new NoSuchElementException("Settlement not found: " + settlementId));

        if (request.getTotalRent() != null) settlement.setTotalRent(request.getTotalRent());
        if (request.getDamageFee() != null) settlement.setDamageFee(request.getDamageFee());
        if (request.getLateFee() != null) settlement.setLateFee(request.getLateFee());
        if (request.getAccessoryFee() != null) settlement.setAccessoryFee(request.getAccessoryFee());
        if (request.getDepositUsed() != null) settlement.setDepositUsed(request.getDepositUsed());
        if (request.getFinalAmount() != null) settlement.setFinalAmount(request.getFinalAmount());
        if (request.getState() != null) {
            settlement.setState(request.getState());
            if (request.getState() == SettlementState.Issued && settlement.getIssuedAt() == null) {
                settlement.setIssuedAt(LocalDateTime.now());
            }
        }

        return settlementRepository.save(settlement);
    }

    @Override
    @Transactional(readOnly = true)
    public Settlement getByOrderId(Long orderId) {
        return settlementRepository.findByRentalOrder_OrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("Settlement not found for orderId: " + orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Settlement> getAll(Pageable pageable) {
        return settlementRepository.findAll(pageable);
    }
}

