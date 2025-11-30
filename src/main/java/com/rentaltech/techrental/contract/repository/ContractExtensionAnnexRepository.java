package com.rentaltech.techrental.contract.repository;

import com.rentaltech.techrental.contract.model.ContractExtensionAnnex;
import com.rentaltech.techrental.contract.model.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractExtensionAnnexRepository extends JpaRepository<ContractExtensionAnnex, Long> {

    Optional<ContractExtensionAnnex> findByAnnexNumber(String annexNumber);

    List<ContractExtensionAnnex> findByContract_ContractId(Long contractId);

    long countByContract_ContractId(Long contractId);

    List<ContractExtensionAnnex> findByContract_ContractIdAndStatus(Long contractId, ContractStatus status);

    Optional<ContractExtensionAnnex> findFirstByExtensionOrder_OrderId(Long orderId);
}
