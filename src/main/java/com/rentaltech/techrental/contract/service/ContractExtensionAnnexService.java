package com.rentaltech.techrental.contract.service;

import com.rentaltech.techrental.contract.model.Contract;
import com.rentaltech.techrental.contract.model.ContractExtensionAnnex;
import com.rentaltech.techrental.contract.model.dto.ContractExtensionAnnexResponseDto;
import com.rentaltech.techrental.contract.model.dto.ContractExtensionAnnexSignRequestDto;

import java.util.List;

public interface ContractExtensionAnnexService {

    ContractExtensionAnnex createAnnexForExtension(Contract contract,
                                                  com.rentaltech.techrental.rentalorder.model.RentalOrderExtension rentalOrderExtension,
                                                  Long createdBy);

    List<ContractExtensionAnnexResponseDto> getAnnexesForContract(Long contractId);

    ContractExtensionAnnexResponseDto getAnnexDetail(Long contractId, Long annexId);

    ContractExtensionAnnexResponseDto signAsAdmin(Long contractId,
                                                  Long annexId,
                                                  Long adminAccountId,
                                                  ContractExtensionAnnexSignRequestDto request);

    ContractExtensionAnnexResponseDto signAsCustomer(Long contractId,
                                                     Long annexId,
                                                     Long customerAccountId,
                                                     ContractExtensionAnnexSignRequestDto request);

    void sendSignaturePinByEmail(Long contractId, Long annexId, String email);
}
