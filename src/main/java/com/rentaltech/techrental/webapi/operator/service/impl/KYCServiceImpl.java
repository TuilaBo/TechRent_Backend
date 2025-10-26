package com.rentaltech.techrental.webapi.operator.service.impl;

import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.KYCStatus;
import com.rentaltech.techrental.webapi.customer.model.dto.KYCVerificationDto;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import com.rentaltech.techrental.webapi.operator.service.KYCService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class KYCServiceImpl implements KYCService {
    
    private final CustomerRepository customerRepository;
    
    @Override
    public Customer uploadDocument(Long customerId, MultipartFile file, String documentType) {
        // Validate customer exists
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer không tồn tại: " + customerId));
        
        // Validate file
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File không được để trống");
        }
        
        // Upload file and get URL
        String imageUrl = saveImageAndReturnUrl(file, customerId, documentType);
        
        // Update customer with image URL
        switch (documentType) {
            case "front_cccd" -> customer.setKycFrontCCCDUrl(imageUrl);
            case "back_cccd" -> customer.setKycBackCCCDUrl(imageUrl);
            case "selfie" -> customer.setKycSelfieUrl(imageUrl);
            default -> throw new RuntimeException("Loại giấy tờ không hợp lệ");
        }
        
        // Update KYC status
        updateKYCStatusAfterUpload(customer);
        
        return customerRepository.save(customer);
    }
    
    @Override
    public Customer updateKYCStatus(Long customerId, KYCVerificationDto request, Long operatorId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer không tồn tại: " + customerId));
        
        customer.setKycStatus(request.getStatus());
        customer.setKycVerifiedAt(LocalDateTime.now());
        customer.setKycVerifiedBy(operatorId);
        
        if (request.getStatus() == KYCStatus.REJECTED) {
            customer.setKycRejectionReason(request.getRejectionReason());
        }
        
        return customerRepository.save(customer);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Customer> getPendingVerification() {
        List<Customer> customers = new ArrayList<>();
        customers.addAll(customerRepository.findByKycStatus(KYCStatus.PENDING_VERIFICATION));
        customers.addAll(customerRepository.findByKycStatus(KYCStatus.DOCUMENTS_SUBMITTED));
        return customers;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Customer getKYCInfo(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer không tồn tại: " + customerId));
    }
    
    /**
     * Save image to storage and return URL
     * TODO: Implement with cloud storage (S3, Azure Blob, etc.)
     */
    private String saveImageAndReturnUrl(MultipartFile file, Long customerId, String imageType) {
        // TODO: Implement actual file upload
        // For now, return mock URL
        return String.format("/uploads/kyc/customer_%d_%s_%d.jpg", 
                customerId, imageType, System.currentTimeMillis());
    }
    
    /**
     * Update KYC status based on uploaded documents
     */
    private void updateKYCStatusAfterUpload(Customer customer) {
        boolean hasFront = customer.getKycFrontCCCDUrl() != null;
        boolean hasBack = customer.getKycBackCCCDUrl() != null;
        boolean hasSelfie = customer.getKycSelfieUrl() != null;
        
        if (hasFront && hasBack && hasSelfie) {
            customer.setKycStatus(KYCStatus.DOCUMENTS_SUBMITTED);
        } else if (hasFront || hasBack || hasSelfie) {
            customer.setKycStatus(KYCStatus.PENDING_VERIFICATION);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> buildKYCMap(Customer customer) {
        Map<String, Object> map = new HashMap<>();
        map.put("customerId", customer.getCustomerId());
        map.put("fullName", customer.getFullName());
        map.put("kycStatus", customer.getKycStatus());
        map.put("frontCCCDUrl", customer.getKycFrontCCCDUrl());
        map.put("backCCCDUrl", customer.getKycBackCCCDUrl());
        map.put("selfieUrl", customer.getKycSelfieUrl());
        map.put("verifiedAt", customer.getKycVerifiedAt());
        map.put("verifiedBy", customer.getKycVerifiedBy());
        map.put("rejectionReason", customer.getKycRejectionReason());
        return map;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> getKYCStatuses() {
        return List.of(KYCStatus.values()).stream()
                .map(status -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("value", status.name());
                    map.put("label", status.getDisplayName());
                    return map;
                })
                .toList();
    }
    
    @Override
    public void validateDocumentType(String documentType) {
        if (!documentType.equals("front_cccd") && 
            !documentType.equals("back_cccd") && 
            !documentType.equals("selfie")) {
            throw new IllegalArgumentException("Loại giấy tờ không hợp lệ. Hợp lệ: front_cccd, back_cccd, selfie");
        }
    }
}

