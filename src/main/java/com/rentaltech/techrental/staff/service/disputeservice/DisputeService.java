package com.rentaltech.techrental.staff.service.disputeservice;

import com.rentaltech.techrental.staff.model.Dispute;
import com.rentaltech.techrental.staff.model.dto.DisputeCreateRequestDto;
import com.rentaltech.techrental.staff.model.dto.DisputeUpdateRequestDto;

public interface DisputeService {
    Dispute create(DisputeCreateRequestDto request);
    Dispute update(Long disputeId, DisputeUpdateRequestDto request);
    Dispute getBySettlementId(Long settlementId);
}

