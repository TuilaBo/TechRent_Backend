package com.rentaltech.techrental.webapi.operator.service.impl;

import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.KYCStatus;
import com.rentaltech.techrental.webapi.customer.model.dto.KYCVerificationDto;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
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
    private final ImageStorageService imageStorageService;
    private final com.rentaltech.techrental.webapi.customer.repository.CustomerRepository custRepo; // alias if needed
    
    @Override
    public Customer uploadDocument(Long customerId, MultipartFile file, String documentType) {
        try {
            // Validate customer exists
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer không tồn tại: " + customerId));

            // Validate file
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("File không được để trống");
            }

            // Upload file and get URL
            String imageUrl = imageStorageService.uploadKycImage(file, customerId, documentType);

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
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Upload KYC thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> operatorUploadDocument(Long customerId, MultipartFile file, String documentType) {
        Customer c = uploadDocument(customerId, file, documentType);
        return buildKYCMap(c);
    }

    @Override
    public Map<String, Object> customerUploadDocument(String username, MultipartFile file, String documentType) {
        try {
            Customer me = customerRepository.findAll().stream()
                    .filter(c -> c.getAccount() != null && c.getAccount().getUsername().equals(username))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Customer không tồn tại"));
            Customer updated = uploadDocument(me.getCustomerId(), file, documentType);
            return buildKYCMap(updated);
        } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException("Upload KYC thất bại: " + e.getMessage(), e); }
    }

    @Override
    public Map<String, Object> customerUploadDocuments(String username, MultipartFile front, MultipartFile back, MultipartFile selfie) {
        try {
            Customer me = customerRepository.findAll().stream()
                    .filter(c -> c.getAccount() != null && c.getAccount().getUsername().equals(username))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Customer không tồn tại"));
            Customer updated = uploadDocuments(me.getCustomerId(), front, back, selfie);
            return buildKYCMap(updated);
        } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException("Upload KYC nhiều ảnh thất bại: " + e.getMessage(), e); }
    }

    @Override
    public Customer uploadDocuments(Long customerId, MultipartFile front, MultipartFile back, MultipartFile selfie) {
        try {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer không tồn tại: " + customerId));

            if ((front == null || front.isEmpty()) && (back == null || back.isEmpty()) && (selfie == null || selfie.isEmpty())) {
                throw new RuntimeException("Ít nhất một ảnh phải được chọn");
            }

            if (front != null && !front.isEmpty()) {
                String url = imageStorageService.uploadKycImage(front, customerId, "front_cccd");
                customer.setKycFrontCCCDUrl(url);
            }
            if (back != null && !back.isEmpty()) {
                String url = imageStorageService.uploadKycImage(back, customerId, "back_cccd");
                customer.setKycBackCCCDUrl(url);
            }
            if (selfie != null && !selfie.isEmpty()) {
                String url = imageStorageService.uploadKycImage(selfie, customerId, "selfie");
                customer.setKycSelfieUrl(url);
            }

            updateKYCStatusAfterUpload(customer);
            return customerRepository.save(customer);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Upload KYC nhiều ảnh thất bại: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Customer updateKYCStatus(Long customerId, KYCVerificationDto request, Long operatorId) {
        try {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer không tồn tại: " + customerId));

            customer.setKycStatus(request.getStatus());
            customer.setKycVerifiedAt(LocalDateTime.now());
            customer.setKycVerifiedBy(operatorId);

            if (request.getStatus() == KYCStatus.REJECTED) {
                customer.setKycRejectionReason(request.getRejectionReason());
            }

            return customerRepository.save(customer);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Cập nhật trạng thái KYC thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> updateKycStatusAndBuild(Long customerId, KYCVerificationDto request, Long operatorId) {
        Customer c = updateKYCStatus(customerId, request, operatorId);
        return buildKYCMap(c);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Customer> getPendingVerification() {
        try {
            List<Customer> customers = new ArrayList<>();
            customers.addAll(customerRepository.findByKycStatus(KYCStatus.PENDING_VERIFICATION));
            customers.addAll(customerRepository.findByKycStatus(KYCStatus.DOCUMENTS_SUBMITTED));
            return customers;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lấy danh sách chờ xác minh thất bại: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> getPendingVerificationMaps() {
        return getPendingVerification().stream().map(this::buildKYCMap).toList();
    }

    @Override
    public Map<String, Object> getMyKyc(String username) {
        Customer me = customerRepository.findAll().stream()
                .filter(c -> c.getAccount() != null && c.getAccount().getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Customer không tồn tại"));
        return buildKYCMap(me);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Customer getKYCInfo(Long customerId) {
        try {
            return customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer không tồn tại: " + customerId));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lấy thông tin KYC thất bại: " + e.getMessage(), e);
        }
    }
    
    /**
     * Save image to storage and return URL
     * TODO: Implement with cloud storage (S3, Azure Blob, etc.)
     */
    // removed local stub uploader; using Cloudinary via ImageStorageService
    
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

