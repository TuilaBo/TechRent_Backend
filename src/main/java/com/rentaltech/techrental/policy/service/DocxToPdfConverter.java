package com.rentaltech.techrental.policy.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Chuyển đổi file DOC/DOCX sang PDF.
 * Triển khai mặc định dùng LibreOffice (soffice) ở chế độ headless trên server.
 */
public interface DocxToPdfConverter {

    /**
     * Convert file Word sang PDF, trả về MultipartFile PDF tạm để upload tiếp lên Cloudinary.
     */
    MultipartFile convertToPdf(MultipartFile docFile);
}


