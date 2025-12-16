package com.rentaltech.techrental.webapi.operator.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    String uploadKycImage(MultipartFile file, Long customerId, String documentType);

    String uploadDeviceModelImage(MultipartFile file, Long brandId, String deviceName);

    String uploadQcAccessorySnapshot(MultipartFile file, Long taskId);

    String uploadHandoverEvidence(MultipartFile file, Long taskId, String label);

    String uploadMaintenanceEvidence(MultipartFile file, Long maintenanceScheduleId, String label);

    String uploadInvoiceProof(MultipartFile file, Long settlementId);

    String uploadPolicyFile(MultipartFile file, Long policyId, String fileName);

    String uploadPolicyPdf(MultipartFile file, Long policyId, String fileName);

    String uploadComplaintEvidence(MultipartFile file, Long complaintId);
}


