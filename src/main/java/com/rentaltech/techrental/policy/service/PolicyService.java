package com.rentaltech.techrental.policy.service;

import com.rentaltech.techrental.policy.model.dto.PolicyFileResponseDto;
import com.rentaltech.techrental.policy.model.dto.PolicyRequestDto;
import com.rentaltech.techrental.policy.model.dto.PolicyResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PolicyService {
    PolicyResponseDto create(PolicyRequestDto request, MultipartFile file, String username);
    PolicyResponseDto update(Long policyId, PolicyRequestDto request, MultipartFile file, String username);
    void delete(Long policyId, String username);
    PolicyResponseDto getById(Long policyId);
    List<PolicyResponseDto> getAll();
    List<PolicyResponseDto> getActivePolicies();
    PolicyFileResponseDto getPolicyFileForView(Long policyId) throws IOException;
    PolicyFileResponseDto getPolicyFileForDownload(Long policyId) throws IOException;
}

