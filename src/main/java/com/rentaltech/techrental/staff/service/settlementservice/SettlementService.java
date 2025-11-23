package com.rentaltech.techrental.staff.service.settlementservice;

import com.rentaltech.techrental.staff.model.Settlement;
import com.rentaltech.techrental.staff.model.dto.SettlementCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.SettlementUpdateRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SettlementService {
    Settlement create(SettlementCreateRequestDto request);
    Settlement createAutomaticForOrder(Long orderId);
    Settlement update(Long settlementId, SettlementUpdateRequestDto request);
    Settlement getByOrderId(Long orderId);
    Page<Settlement> getAll(Pageable pageable);
    Settlement respondToSettlement(Long settlementId, boolean accepted, String username);
}

