package com.rentaltech.techrental.webapi.operator.service;

import org.springframework.web.multipart.MultipartFile;

public interface OcrService {
    String extractText(MultipartFile imageFile, String language);
}



