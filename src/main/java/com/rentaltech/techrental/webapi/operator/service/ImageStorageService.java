package com.rentaltech.techrental.webapi.operator.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    String uploadKycImage(MultipartFile file, Long customerId, String documentType);

    String uploadDeviceModelImage(MultipartFile file, Long brandId, String deviceName);

    String uploadQcAccessorySnapshot(MultipartFile file, Long taskId);
}
