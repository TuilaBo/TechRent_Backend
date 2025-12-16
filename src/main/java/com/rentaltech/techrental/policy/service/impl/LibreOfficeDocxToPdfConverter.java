package com.rentaltech.techrental.policy.service.impl;

import com.rentaltech.techrental.policy.service.DocxToPdfConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Lightweight MultipartFile implementation backed by a byte[]
 * để tránh phụ thuộc spring-test (MockMultipartFile).
 */
class ByteArrayMultipartFile implements MultipartFile {
    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final byte[] bytes;

    ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] bytes) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.bytes = bytes != null ? bytes : new byte[0];
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return bytes.length == 0;
    }

    @Override
    public long getSize() {
        return bytes.length;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public java.io.InputStream getInputStream() {
        return new java.io.ByteArrayInputStream(bytes);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        Files.write(dest.toPath(), bytes);
    }
}

/**
 * Triển khai chuyển đổi DOC/DOCX sang PDF bằng LibreOffice (soffice) ở chế độ headless.
 * YÊU CẦU: máy chạy server phải cài sẵn LibreOffice và lệnh "soffice" nằm trong PATH.
 */
@Component
@Slf4j
public class LibreOfficeDocxToPdfConverter implements DocxToPdfConverter {

    @Override
    public MultipartFile convertToPdf(MultipartFile docFile) {
        if (docFile == null || docFile.isEmpty()) {
            throw new IllegalArgumentException("File DOC/DOCX không được rỗng");
        }
        File tempInput = null;
        File tempOutputDir = null;
        try {
            String originalName = docFile.getOriginalFilename();
            String suffix = originalName != null && originalName.toLowerCase().endsWith(".docx") ? ".docx" : ".doc";
            tempInput = File.createTempFile("policy-src-" + UUID.randomUUID(), suffix);
            Files.write(tempInput.toPath(), docFile.getBytes());

            tempOutputDir = Files.createTempDirectory("policy-pdf-" + UUID.randomUUID()).toFile();

            ProcessBuilder pb = new ProcessBuilder(
                    "soffice",
                    "--headless",
                    "--convert-to", "pdf",
                    "--outdir", tempOutputDir.getAbsolutePath(),
                    tempInput.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("LibreOffice chuyển đổi PDF thất bại, exitCode=" + exitCode);
            }

            // Tìm file PDF trong thư mục output
            File[] files = tempOutputDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            if (files == null || files.length == 0) {
                throw new IllegalStateException("Không tìm thấy file PDF sau khi convert");
            }
            File pdfFile = files[0];
            String pdfName = (originalName != null ? originalName : "policy") + ".pdf";
            byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());
            return new ByteArrayMultipartFile(
                    pdfName,
                    pdfName,
                    MediaType.APPLICATION_PDF_VALUE,
                    pdfBytes
            );
        } catch (IOException | InterruptedException e) {
            log.error("Lỗi khi convert DOC/DOCX sang PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể convert DOC/DOCX sang PDF: " + e.getMessage(), e);
        } finally {
            if (tempInput != null && tempInput.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tempInput.delete();
            }
            if (tempOutputDir != null && tempOutputDir.exists()) {
                File[] files = tempOutputDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        //noinspection ResultOfMethodCallIgnored
                        f.delete();
                    }
                }
                //noinspection ResultOfMethodCallIgnored
                tempOutputDir.delete();
            }
        }
    }
}


