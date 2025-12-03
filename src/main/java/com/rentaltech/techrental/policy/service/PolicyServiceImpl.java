package com.rentaltech.techrental.policy.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.policy.model.Policy;
import com.rentaltech.techrental.policy.model.dto.PolicyRequestDto;
import com.rentaltech.techrental.policy.model.dto.PolicyResponseDto;
import com.rentaltech.techrental.policy.repository.PolicyRepository;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

        if (file != null && !file.isEmpty()) {
            String fileName = file.getOriginalFilename();
            String fileType = extractFileType(fileName);
            String fileUrl = imageStorageService.uploadPolicyFile(file, null, fileName);
            policy.setFileUrl(fileUrl);
            policy.setFileName(fileName);
            policy.setFileType(fileType);
        }

        Policy saved = policyRepository.save(policy);
        
        // Update file URL with policy ID if needed
        if (saved.getFileUrl() == null && file != null && !file.isEmpty()) {
            String fileName = file.getOriginalFilename();
            String fileType = extractFileType(fileName);
            String fileUrl = imageStorageService.uploadPolicyFile(file, saved.getPolicyId(), fileName);
            saved.setFileUrl(fileUrl);
            saved.setFileName(fileName);
            saved.setFileType(fileType);
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
            String fileUrl = imageStorageService.uploadPolicyFile(file, policyId, fileName);
            existing.setFileUrl(fileUrl);
            existing.setFileName(fileName);
            existing.setFileType(fileType);
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

