package com.rentaltech.techrental.webapi.operator.service.impl;

import com.rentaltech.techrental.webapi.operator.service.OcrService;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    @Value("${ocr.tesseract.datapath:}")
    private String tessDataPath;

    @Override
    public String extractText(MultipartFile imageFile, String language) {
        if (imageFile == null || imageFile.isEmpty()) throw new IllegalArgumentException("Cần cung cấp tệp hình ảnh");
        String lang = (language == null || language.isBlank()) ? "eng" : language;
        Tesseract tesseract = new Tesseract();
        if (tessDataPath != null && !tessDataPath.isBlank()) tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage(lang);
        File temp = null;
        try {
            // Determine file extension based on content type
            String extension = ".png";
            String contentType = imageFile.getContentType();
            if (contentType != null) {
                if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                    extension = ".jpg";
                }
            }
            temp = File.createTempFile("kyc-ocr-", extension);
            Path p = temp.toPath();
            Files.copy(imageFile.getInputStream(), p, StandardCopyOption.REPLACE_EXISTING);
            return tesseract.doOCR(temp);
        } catch (IOException | TesseractException e) {
            throw new RuntimeException("Nhận dạng OCR thất bại: " + e.getMessage(), e);
        } finally {
            if (temp != null) temp.delete();
        }
    }
}



