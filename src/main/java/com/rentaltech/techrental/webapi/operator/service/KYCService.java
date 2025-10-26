package com.rentaltech.techrental.webapi.operator.service;

import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.dto.KYCVerificationDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface KYCService {
    
    /**
     * Upload KYC document for customer
     */
    Customer uploadDocument(Long customerId, MultipartFile file, String documentType);
    
    /**
     * Update KYC status (verify or reject)
     */
    Customer updateKYCStatus(Long customerId, KYCVerificationDto request, Long operatorId);
    
    /**
     * Get list of customers pending verification
     */
    List<Customer> getPendingVerification();
    
    /**
     * Get KYC info for customer
     */
    Customer getKYCInfo(Long customerId);
    
    /**
     * Build KYC information map from customer
     */
    Map<String, Object> buildKYCMap(Customer customer);
    
    /**
     * Get all KYC statuses as list of maps (value, label)
     */
    List<Map<String, String>> getKYCStatuses();
    
    /**
     * Validate document type
     */
    void validateDocumentType(String documentType);
}

