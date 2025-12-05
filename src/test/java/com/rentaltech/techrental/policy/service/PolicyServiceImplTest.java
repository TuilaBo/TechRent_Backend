package com.rentaltech.techrental.policy.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.policy.model.Policy;
import com.rentaltech.techrental.policy.model.dto.PolicyRequestDto;
import com.rentaltech.techrental.policy.model.dto.PolicyResponseDto;
import com.rentaltech.techrental.policy.repository.PolicyRepository;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceImplTest {

    @Mock
    private PolicyRepository policyRepository;
    @Mock
    private ImageStorageService imageStorageService;
    @Mock
    private AccountService accountService;

    @InjectMocks
    private PolicyServiceImpl policyService;

    private PolicyRequestDto baseRequest;

    @BeforeEach
    void setUp() {
        baseRequest = PolicyRequestDto.builder()
                .title("Policy A")
                .description("Desc")
                .effectiveFrom(LocalDate.now())
                .effectiveTo(LocalDate.now().plusDays(10))
                .build();
    }

    @Test
    void createRejectsInvalidEffectiveDates() {
        baseRequest.setEffectiveFrom(LocalDate.now().plusDays(5));
        baseRequest.setEffectiveTo(LocalDate.now());

        assertThatThrownBy(() -> policyService.create(baseRequest, null, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectiveFrom");
    }

    @Test
    void createUploadsFileAndSavesPolicy() {
        when(accountService.getByUsername("creator"))
                .thenReturn(Optional.of(Account.builder().username("creator").build()));
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> {
            Policy policy = invocation.getArgument(0);
            if (policy.getPolicyId() == null) {
                policy.setPolicyId(100L);
            }
            return policy;
        });
        MockMultipartFile file = new MockMultipartFile("file", "policy.pdf", "application/pdf", new byte[]{1});
        when(imageStorageService.uploadPolicyFile(any(MultipartFile.class), isNull(), anyString()))
                .thenReturn("https://files/policy.pdf");

        PolicyResponseDto response = policyService.create(baseRequest, file, "creator");

        assertThat(response.getPolicyId()).isEqualTo(100L);
        ArgumentCaptor<Policy> captor = ArgumentCaptor.forClass(Policy.class);
        verify(policyRepository).save(captor.capture());
        Policy saved = captor.getValue();
        assertThat(saved.getFileName()).isEqualTo("policy.pdf");
        assertThat(saved.getFileType()).isEqualTo("PDF");
    }

    @Test
    void createRejectsUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile("file", "policy.txt", "text/plain", new byte[]{1});
        assertThatThrownBy(() -> policyService.create(baseRequest, file, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("file");
    }

    @Test
    void updateReplacesMetadataAndFile() {
        when(accountService.getByUsername("creator"))
                .thenReturn(Optional.of(Account.builder().username("creator").build()));
        Policy existing = Policy.builder()
                .policyId(55L)
                .title("Old")
                .description("Old")
                .effectiveFrom(LocalDate.now())
                .effectiveTo(LocalDate.now().plusDays(5))
                .build();
        when(policyRepository.findByPolicyIdAndDeletedAtIsNull(55L)).thenReturn(Optional.of(existing));
        when(policyRepository.save(any(Policy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageStorageService.uploadPolicyFile(any(MultipartFile.class), eq(55L), anyString())).thenReturn("url2");
        MockMultipartFile file = new MockMultipartFile("file", "policy.doc", "application/msword", new byte[]{2});

        PolicyResponseDto response = policyService.update(55L, baseRequest, file, "creator");

        assertThat(response.getPolicyId()).isEqualTo(55L);
        assertThat(existing.getTitle()).isEqualTo(baseRequest.getTitle());
        assertThat(existing.getFileType()).isEqualTo("DOC");
    }

    @Test
    void deleteMarksPolicyAsDeleted() {
        when(accountService.getByUsername("creator"))
                .thenReturn(Optional.of(Account.builder().username("creator").build()));
        Policy existing = Policy.builder()
                .policyId(77L)
                .createdAt(LocalDateTime.now())
                .build();
        when(policyRepository.findByPolicyIdAndDeletedAtIsNull(77L)).thenReturn(Optional.of(existing));
        when(policyRepository.save(existing)).thenAnswer(invocation -> invocation.getArgument(0));

        policyService.delete(77L, "creator");

        assertThat(existing.getDeletedAt()).isNotNull();
        assertThat(existing.getDeletedBy()).isEqualTo("creator");
        verify(policyRepository).save(existing);
    }
}
