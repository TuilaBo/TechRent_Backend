package com.rentaltech.techrental.policy.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.policy.model.Policy;
import com.rentaltech.techrental.policy.model.dto.PolicyFileResponseDto;
import com.rentaltech.techrental.policy.model.dto.PolicyRequestDto;
import com.rentaltech.techrental.policy.model.dto.PolicyResponseDto;
import com.rentaltech.techrental.policy.repository.PolicyRepository;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;
    private final ImageStorageService imageStorageService;
    private final AccountService accountService;
    private final DocxToPdfConverter docxToPdfConverter;

    @Override
    @Transactional
    public PolicyResponseDto create(PolicyRequestDto request, MultipartFile file, String username) {
        validateEffectiveDates(request.getEffectiveFrom(), request.getEffectiveTo());
        validateFile(file);

        Policy policy = Policy.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .createdBy(resolveCreator(username))
                .createdAt(LocalDateTime.now())
                .build();

        Policy saved = policyRepository.save(policy);

        // Nếu có file Word, lưu cả bản gốc và convert sang PDF cho end-user xem
        if (file != null && !file.isEmpty()) {
            String fileName = file.getOriginalFilename();
            String fileType = extractFileType(fileName);
            // Lưu file gốc (DOC/DOCX)
            String originalUrl = imageStorageService.uploadPolicyFile(file, saved.getPolicyId(), fileName);
            saved.setOriginalFileUrl(originalUrl);

            // Nếu là DOC/DOCX thì convert sang PDF, upload và set fileUrl = PDF
            if ("DOC".equalsIgnoreCase(fileType) || "DOCX".equalsIgnoreCase(fileType)) {
                org.springframework.web.multipart.MultipartFile pdfFile = docxToPdfConverter.convertToPdf(file);
                String pdfUrl = imageStorageService.uploadPolicyPdf(pdfFile, saved.getPolicyId(), fileName + ".pdf");
                saved.setFileUrl(pdfUrl);
                saved.setFileName(pdfFile.getOriginalFilename());
                saved.setFileType("PDF");
            } else if ("PDF".equalsIgnoreCase(fileType)) {
                // Trường hợp upload trực tiếp PDF
                saved.setFileUrl(originalUrl);
                saved.setFileName(fileName);
                saved.setFileType("PDF");
            } else {
                // Các loại khác: chỉ lưu original, không convert
                saved.setFileUrl(originalUrl);
                saved.setFileName(fileName);
                saved.setFileType(fileType);
            }

            saved = policyRepository.save(saved);
        }

        return PolicyResponseDto.from(saved);
    }

    @Override
    @Transactional
    public PolicyResponseDto update(Long policyId, PolicyRequestDto request, MultipartFile file, String username) {
        Policy existing = policyRepository.findByPolicyIdAndDeletedAtIsNull(policyId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy policy với id " + policyId));

        validateEffectiveDates(request.getEffectiveFrom(), request.getEffectiveTo());

        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setEffectiveFrom(request.getEffectiveFrom());
        existing.setEffectiveTo(request.getEffectiveTo());
        existing.setUpdatedBy(resolveCreator(username));
        existing.setUpdatedAt(LocalDateTime.now());

        if (file != null && !file.isEmpty()) {
            validateFile(file);
            String fileName = file.getOriginalFilename();
            String fileType = extractFileType(fileName);
            // Cập nhật file gốc
            String originalUrl = imageStorageService.uploadPolicyFile(file, policyId, fileName);
            existing.setOriginalFileUrl(originalUrl);

            if ("DOC".equalsIgnoreCase(fileType) || "DOCX".equalsIgnoreCase(fileType)) {
                org.springframework.web.multipart.MultipartFile pdfFile = docxToPdfConverter.convertToPdf(file);
                String pdfUrl = imageStorageService.uploadPolicyPdf(pdfFile, policyId, fileName + ".pdf");
                existing.setFileUrl(pdfUrl);
                existing.setFileName(pdfFile.getOriginalFilename());
                existing.setFileType("PDF");
            } else if ("PDF".equalsIgnoreCase(fileType)) {
                existing.setFileUrl(originalUrl);
                existing.setFileName(fileName);
                existing.setFileType("PDF");
            } else {
                existing.setFileUrl(originalUrl);
                existing.setFileName(fileName);
                existing.setFileType(fileType);
            }
        }

        Policy saved = policyRepository.save(existing);
        return PolicyResponseDto.from(saved);
    }

    @Override
    @Transactional
    public void delete(Long policyId, String username) {
        Policy policy = policyRepository.findByPolicyIdAndDeletedAtIsNull(policyId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy policy với id " + policyId));

        policy.setDeletedBy(resolveCreator(username));
        policy.setDeletedAt(LocalDateTime.now());
        policyRepository.save(policy);
    }

    @Override
    public PolicyResponseDto getById(Long policyId) {
        Policy policy = policyRepository.findByPolicyIdAndDeletedAtIsNull(policyId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy policy với id " + policyId));
        return PolicyResponseDto.from(policy);
    }

    @Override
    public List<PolicyResponseDto> getAll() {
        return policyRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(PolicyResponseDto::from)
                .toList();
    }

    @Override
    public List<PolicyResponseDto> getActivePolicies() {
        return policyRepository.findActivePolicies().stream()
                .map(PolicyResponseDto::from)
                .toList();
    }

    @Override
    public PolicyFileResponseDto getPolicyFileForView(Long policyId) throws IOException {
        PolicyResponseDto policy = getById(policyId);
        if (policy.getFileUrl() == null) {
            throw new NoSuchElementException("Policy không có file");
        }

        byte[] bytes = downloadFileFromUrl(policy.getFileUrl());
        String filename = policy.getFileName() != null && !policy.getFileName().isBlank()
                ? policy.getFileName()
                : "policy-" + policyId + ".pdf";

        return PolicyFileResponseDto.builder()
                .content(bytes)
                .filename(filename)
                .contentType(MediaType.APPLICATION_PDF)
                .contentDisposition(
                        ContentDisposition.inline()
                                .filename(filename, StandardCharsets.UTF_8)
                                .build()
                )
                .cacheControl("public, max-age=3600")
                .build();
    }

    @Override
    public PolicyFileResponseDto getPolicyFileForDownload(Long policyId) throws IOException {
        PolicyResponseDto policy = getById(policyId);
        if (policy.getOriginalFileUrl() == null) {
            throw new NoSuchElementException("Policy không có file gốc");
        }

        byte[] bytes = downloadFileFromUrl(policy.getOriginalFileUrl());
        String extension = extractExtensionFromUrl(policy.getOriginalFileUrl());
        String fileTypeFromUrl = extension.toUpperCase();

        String filename;
        if (policy.getFileName() != null && !policy.getFileName().isBlank()) {
            String baseName = policy.getFileName().replaceAll("\\.pdf$", "").replaceAll("\\.(doc|docx)$", "");
            filename = baseName + "." + extension;
        } else {
            filename = "policy-" + policyId + "." + extension;
        }

        MediaType contentType = switch (fileTypeFromUrl) {
            case "DOC" -> MediaType.parseMediaType("application/msword");
            case "DOCX" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "PDF" -> MediaType.APPLICATION_PDF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };

        return PolicyFileResponseDto.builder()
                .content(bytes)
                .filename(filename)
                .contentType(contentType)
                .contentDisposition(
                        ContentDisposition.attachment()
                                .filename(filename, StandardCharsets.UTF_8)
                                .build()
                )
                .cacheControl(null)
                .build();
    }

    private byte[] downloadFileFromUrl(String fileUrl) throws IOException {
        URL url = new URL(fileUrl);
        return url.openStream().readAllBytes();
    }

    private String extractExtensionFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return "docx";
        }
        String path = url.split("\\?")[0];
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0 && lastDot < path.length() - 1) {
            return path.substring(lastDot + 1).toLowerCase();
        }
        return "docx";
    }

    private void validateEffectiveDates(java.time.LocalDate effectiveFrom, java.time.LocalDate effectiveTo) {
        if (effectiveFrom != null && effectiveTo != null) {
            if (effectiveFrom.isAfter(effectiveTo)) {
                throw new IllegalArgumentException("effectiveFrom không được sau effectiveTo");
            }
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return; // File is optional
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Tên file không hợp lệ");
        }
        String fileType = extractFileType(fileName).toUpperCase();
        if (!fileType.equals("PDF") && !fileType.equals("DOC") && !fileType.equals("DOCX")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file PDF, DOC hoặc DOCX");
        }
    }

    private String extractFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "UNKNOWN";
        }
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
        return extension;
    }

    private String resolveCreator(String username) {
        if (username == null) {
            return null;
        }
        Optional<Account> account = accountService.getByUsername(username);
        return account.map(Account::getUsername).orElse(username);
    }
}

