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

    private String uploadFile(MultipartFile file, Map<String, Object> options) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp rỗng");
        }
        try {
            Map<String, Object> upload = cloudinary.uploader().upload(file.getBytes(), options);
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


