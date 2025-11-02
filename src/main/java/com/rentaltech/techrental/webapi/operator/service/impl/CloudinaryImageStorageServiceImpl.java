package com.rentaltech.techrental.webapi.operator.service.impl;

import com.cloudinary.Cloudinary;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryImageStorageServiceImpl implements ImageStorageService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadKycImage(MultipartFile file, Long customerId, String documentType) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Tệp rỗng");
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("folder", String.format("techrental/kyc/customer_%d", customerId));
            options.put("public_id", String.format("%s_%d", documentType, System.currentTimeMillis()));
            options.put("overwrite", true);
            Map<String, Object> upload = cloudinary.uploader().upload(file.getBytes(), options);
            Object secureUrl = upload.get("secure_url");
            if (secureUrl == null) throw new IllegalStateException("Cloudinary không trả về secure_url");
            return secureUrl.toString();
        } catch (IOException e) {
            throw new RuntimeException("Tải lên Cloudinary thất bại: " + e.getMessage(), e);
        }
    }
}


