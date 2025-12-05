package com.rentaltech.techrental.webapi.operator.service.impl;

import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.staff.service.PreRentalQcTaskCreator;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.KYCStatus;
import com.rentaltech.techrental.webapi.customer.model.dto.KYCVerificationDto;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import com.rentaltech.techrental.webapi.operator.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KYCServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ImageStorageService imageStorageService;
    @Mock
    private RentalOrderRepository rentalOrderRepository;
    @Mock
    private PreRentalQcTaskCreator preRentalQcTaskCreator;
    @Mock
    private MultipartFile frontFile;
    @Mock
    private MultipartFile backFile;
    @Mock
    private MultipartFile selfieFile;

    private KYCServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new KYCServiceImpl(
                customerRepository,
                imageStorageService,
                customerRepository,
                rentalOrderRepository,
                preRentalQcTaskCreator
        );
    }

    @Test
    void uploadDocumentsRequiresAtLeastOneFile() {
        Customer customer = Customer.builder().customerId(1L).build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.uploadDocuments(1L, null, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ít nhất một ảnh");
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void uploadDocumentsUploadsProvidedFilesAndUpdatesStatus() {
        Customer customer = Customer.builder()
                .customerId(10L)
                .kycStatus(KYCStatus.NOT_STARTED)
                .build();
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(frontFile.isEmpty()).thenReturn(false);
        when(backFile.isEmpty()).thenReturn(false);
        when(selfieFile.isEmpty()).thenReturn(false);
        when(imageStorageService.uploadKycImage(frontFile, 10L, "front_cccd")).thenReturn("front-url");
        when(imageStorageService.uploadKycImage(backFile, 10L, "back_cccd")).thenReturn("back-url");
        when(imageStorageService.uploadKycImage(selfieFile, 10L, "selfie")).thenReturn("selfie-url");
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer updated = service.uploadDocuments(10L, frontFile, backFile, selfieFile);

        assertThat(updated.getKycFrontCCCDUrl()).isEqualTo("front-url");
        assertThat(updated.getKycBackCCCDUrl()).isEqualTo("back-url");
        assertThat(updated.getKycSelfieUrl()).isEqualTo("selfie-url");
        assertThat(updated.getKycStatus()).isEqualTo(KYCStatus.DOCUMENTS_SUBMITTED);
    }

    @Test
    void updateKYCStatusToVerifiedActivatesPendingOrders() {
        Customer customer = Customer.builder()
                .customerId(50L)
                .kycStatus(KYCStatus.PENDING_VERIFICATION)
                .build();
        when(customerRepository.findById(50L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RentalOrder pendingOrder = RentalOrder.builder()
                .orderId(200L)
                .orderStatus(OrderStatus.PENDING_KYC)
                .build();
        when(rentalOrderRepository.findByCustomer_CustomerIdAndOrderStatus(50L, OrderStatus.PENDING_KYC))
                .thenReturn(List.of(pendingOrder));

        KYCVerificationDto request = KYCVerificationDto.builder()
                .status(KYCStatus.VERIFIED)
                .build();

        Customer result = service.updateKYCStatus(50L, request, 99L);

        assertThat(result.getKycStatus()).isEqualTo(KYCStatus.VERIFIED);
        assertThat(result.getKycVerifiedBy()).isEqualTo(99L);
        verify(rentalOrderRepository).saveAll(List.of(pendingOrder));
        assertThat(pendingOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        verify(preRentalQcTaskCreator).createIfNeeded(200L);
    }

    @Test
    void updateKYCStatusToRejectedStoresReason() {
        Customer customer = Customer.builder()
                .customerId(70L)
                .kycStatus(KYCStatus.DOCUMENTS_SUBMITTED)
                .build();
        when(customerRepository.findById(70L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KYCVerificationDto request = KYCVerificationDto.builder()
                .status(KYCStatus.REJECTED)
                .rejectionReason("Blurry")
                .build();

        Customer result = service.updateKYCStatus(70L, request, 15L);

        assertThat(result.getKycStatus()).isEqualTo(KYCStatus.REJECTED);
        assertThat(result.getKycRejectionReason()).isEqualTo("Blurry");
        verify(rentalOrderRepository, never()).findByCustomer_CustomerIdAndOrderStatus(any(), any());
    }

    @Test
    void getPendingVerificationCombinesStatuses() {
        Customer pending = Customer.builder().customerId(1L).build();
        Customer submitted = Customer.builder().customerId(2L).build();
        when(customerRepository.findByKycStatus(KYCStatus.PENDING_VERIFICATION)).thenReturn(List.of(pending));
        when(customerRepository.findByKycStatus(KYCStatus.DOCUMENTS_SUBMITTED)).thenReturn(List.of(submitted));

        List<Customer> result = service.getPendingVerification();

        assertThat(result).containsExactlyInAnyOrder(pending, submitted);
    }

    @Test
    void getKycStatusesReturnsValueLabelPairs() {
        List<java.util.Map<String, String>> statuses = service.getKYCStatuses();
        KYCStatus[] expected = KYCStatus.values();
        assertThat(statuses).hasSize(expected.length);
        assertThat(statuses).allSatisfy(map -> {
            assertThat(map).containsKeys("value", "label");
            assertThat(map.get("value")).isIn(
                    java.util.Arrays.stream(expected).map(Enum::name).toList()
            );
        });
    }

    @Test
    void validateDocumentTypeRejectsUnknownType() {
        assertThatThrownBy(() -> service.validateDocumentType("passport"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
