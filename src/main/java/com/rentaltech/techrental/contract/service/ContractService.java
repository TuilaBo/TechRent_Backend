package com.rentaltech.techrental.contract.service;

import com.rentaltech.techrental.contract.model.Contract;
import com.rentaltech.techrental.contract.model.ContractStatus;
import com.rentaltech.techrental.contract.model.dto.ContractCreateRequestDto;
import com.rentaltech.techrental.contract.model.dto.DigitalSignatureRequestDto;
import com.rentaltech.techrental.contract.model.dto.DigitalSignatureResponseDto;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

public interface ContractService {
    
    // Contract CRUD
    List<Contract> getAllContracts();
    Optional<Contract> getContractById(Long contractId);
    Optional<Contract> getContractByNumber(String contractNumber);
    List<Contract> getContractsByCustomerId(Long customerId);
    List<Contract> getContractsByStatus(ContractStatus status);
    List<Contract> getContractsByCustomerIdAndStatus(Long customerId, ContractStatus status);
    
    Contract createContract(ContractCreateRequestDto request, Long createdBy);
    Contract createContractFromOrder(Long orderId, Long createdBy);
    Contract updateContract(Long contractId, ContractCreateRequestDto request, Long updatedBy);
    void deleteContract(Long contractId);
    
    // Contract Status Management
    Contract updateContractStatus(Long contractId, ContractStatus status);
    Contract sendForSignature(Long contractId, Long sentBy);
    Contract cancelContract(Long contractId, String reason, Long cancelledBy);
    
    // PIN Code Management
    ResponseEntity<?> sendSMSPIN(Long contractId, String phoneNumber);
    ResponseEntity<?> sendEmailPIN(Long contractId, String email);
    
    // Digital Signature
    DigitalSignatureResponseDto signContract(DigitalSignatureRequestDto request);
    DigitalSignatureResponseDto signContractByAdmin(Long contractId, Long adminId, DigitalSignatureRequestDto request);
    boolean verifySignature(Long contractId);
    DigitalSignatureResponseDto getSignatureInfo(Long contractId);
    
    // Contract Lifecycle
    List<Contract> getExpiredContracts();
    List<Contract> getContractsExpiringSoon(int days);
    Contract renewContract(Long contractId, int additionalDays);
    
    // Validation
    boolean validateContractForSignature(Long contractId);
    boolean validateSignatureData(DigitalSignatureRequestDto request);
    
    // Audit
    String generateContractNumber();
    String generateAuditTrail(Long contractId);
}
