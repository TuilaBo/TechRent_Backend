package com.rentaltech.techrental.contract.service;

import com.rentaltech.techrental.contract.model.DeviceContractTerm;
import com.rentaltech.techrental.contract.model.dto.DeviceContractTermRequestDto;
import com.rentaltech.techrental.contract.model.dto.DeviceContractTermResponseDto;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;

import java.util.List;

public interface DeviceContractTermService {

    DeviceContractTermResponseDto create(DeviceContractTermRequestDto request, Long adminId);

    DeviceContractTermResponseDto update(Long termId, DeviceContractTermRequestDto request, Long adminId);

    void delete(Long termId);

    DeviceContractTermResponseDto get(Long termId);

    List<DeviceContractTermResponseDto> list(Long deviceId, Long deviceCategoryId, Boolean active);

    List<DeviceContractTerm> findApplicableTerms(RentalOrder order, List<OrderDetail> orderDetails);
}

