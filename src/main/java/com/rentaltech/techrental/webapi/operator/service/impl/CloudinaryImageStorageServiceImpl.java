package com.rentaltech.techrental.webapi.operator.service.impl;

import com.cloudinary.Cloudinary;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryImageStorageServiceImpl implements ImageStorageService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadKycImage(MultipartFile file, Long customerId, String documentType) {
        String folder = String.format("techrental/kyc/customer_%d", customerId != null ? customerId : 0);
        String document = (documentType == null || documentType.isBlank()) ? "document" : documentType;
        return uploadFile(file, buildOptions(folder, document));
    }

    @Override
    public String uploadDeviceModelImage(MultipartFile file, Long brandId, String deviceName) {
        String folder = brandId != null
                ? String.format("techrental/device-models/brand_%d", brandId)
                : "techrental/device-models";
        String safeName = slugify(deviceName, "device-model");
        return uploadFile(file, buildOptions(folder, safeName));
    }

    @Override
    public String uploadQcAccessorySnapshot(MultipartFile file, Long taskId) {
        String folder = taskId != null
                ? String.format("techrental/qc/task_%d", taskId)
                : "techrental/qc";
        return uploadFile(file, buildOptions(folder, "accessory-snapshot"));
    }

    @Override
    public String uploadHandoverEvidence(MultipartFile file, Long taskId, String label) {
        String folder = taskId != null
                ? String.format("techrental/handover/task_%d", taskId)
                : "techrental/handover";
        String safeName = slugify(label, "handover-evidence");
        return uploadFile(file, buildOptions(folder, safeName));
    }

    @Override
    public String uploadMaintenanceEvidence(MultipartFile file, Long maintenanceScheduleId, String label) {
        String folder = maintenanceScheduleId != null
                ? String.format("techrental/maintenance/schedule_%d", maintenanceScheduleId)
                : "techrental/maintenance";
        String safeName = slugify(label, "maintenance-evidence");
        return uploadFile(file, buildOptions(folder, safeName));
    }

    @Override
    public String uploadInvoiceProof(MultipartFile file, Long settlementId) {
        String folder = settlementId != null
                ? String.format("techrental/finance/settlement_%d", settlementId)
                : "techrental/finance";
        return uploadFile(file, buildOptions(folder, "invoice-proof"));
    }

    @Override
    public String uploadPolicyFile(MultipartFile file, Long policyId, String fileName) {
        String folder = policyId != null
                ? String.format("techrental/policy/policy_%d", policyId)
                : "techrental/policy";
        String safeName = slugify(fileName, "policy-file");
        Map<String, Object> options = buildOptions(folder, safeName);
        // Word documents - lưu dạng raw
        options.put("resource_type", "raw");
        return uploadFile(file, options);
    }

    @Override
    public String uploadPolicyPdf(MultipartFile file, Long policyId, String fileName) {
        String folder = policyId != null
                ? String.format("techrental/policy/policy_%d", policyId)
                : "techrental/policy";
        String safeName = slugify(fileName, "policy-pdf");
        Map<String, Object> options = buildOptions(folder, safeName);
        // PDF cũng để ở resource_type=raw để tải trực tiếp
        options.put("resource_type", "raw");
        return uploadFile(file, options);
    }

    @Override
    public String uploadComplaintEvidence(MultipartFile file, Long complaintId) {
        String folder = complaintId != null
                ? String.format("techrental/complaints/complaint_%d", complaintId)
                : "techrental/complaints";
        return uploadFile(file, buildOptions(folder, "evidence"));
    }

    private String uploadFile(MultipartFile file, Map<String, Object> options) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp rỗng");
        }
        try {
            Map<?, ?> upload = cloudinary.uploader().upload(file.getBytes(), options);
            Object secureUrl = upload.get("secure_url");
            if (secureUrl == null) throw new IllegalStateException("Cloudinary không trả về secure_url");
            return secureUrl.toString();
        } catch (IOException e) {
            throw new RuntimeException("Tải lên Cloudinary thất bại: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildOptions(String folder, String publicIdPrefix) {
        Map<String, Object> options = new HashMap<>();
        options.put("folder", folder);
        options.put("public_id", publicIdPrefix + "_" + System.currentTimeMillis());
        options.put("overwrite", true);
        return options;
    }

    private String slugify(String input, String fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = normalized.replaceAll("[^a-zA-Z0-9]+", "-")
                .replaceAll("(^-|-$)", "")
                .toLowerCase(Locale.ROOT);
        return slug.isBlank() ? fallback : slug;
    }
}


