package com.rentaltech.techrental.contract.service;

import com.rentaltech.techrental.contract.model.Contract;
import com.rentaltech.techrental.contract.model.ContractExtensionAnnex;
import com.rentaltech.techrental.contract.model.dto.ContractExtensionAnnexResponseDto;
import com.rentaltech.techrental.contract.model.dto.ContractExtensionAnnexSignRequestDto;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.model.RentalOrderExtension;

import java.util.List;

public interface ContractExtensionAnnexService {

    ContractExtensionAnnex createAnnexForExtension(Contract contract,
                                                  RentalOrder originalOrder,
                                                  RentalOrderExtension extension,
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
