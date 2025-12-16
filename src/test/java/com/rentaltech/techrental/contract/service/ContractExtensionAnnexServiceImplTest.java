package com.rentaltech.techrental.contract.service;

import com.rentaltech.techrental.contract.model.Contract;
import com.rentaltech.techrental.contract.model.ContractExtensionAnnex;
import com.rentaltech.techrental.contract.model.ContractStatus;
import com.rentaltech.techrental.contract.model.dto.ContractExtensionAnnexResponseDto;
import com.rentaltech.techrental.contract.model.dto.ContractExtensionAnnexSignRequestDto;
import com.rentaltech.techrental.contract.repository.ContractExtensionAnnexRepository;
import com.rentaltech.techrental.contract.service.impl.ContractExtensionAnnexServiceImpl;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.finance.model.Invoice;
import com.rentaltech.techrental.finance.repository.InvoiceRepository;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.model.RentalOrderExtension;
import com.rentaltech.techrental.rentalorder.repository.OrderDetailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractExtensionAnnexServiceImplTest {

    @InjectMocks
    private ContractExtensionAnnexServiceImpl service;

    @Mock
    private ContractExtensionAnnexRepository annexRepository;
    @Mock
    private OrderDetailRepository orderDetailRepository;
    @Mock
    private DigitalSignatureService digitalSignatureService;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private EmailService emailService;

    private Contract contract;
    private RentalOrder originalOrder;
    private RentalOrderExtension rentalOrderExtension;

    @BeforeEach
    void setUp() {
        contract = Contract.builder()
                .contractId(1L)
                .contractNumber("HD202401010001")
                .startDate(LocalDateTime.now().minusDays(10))
                .build();
        originalOrder = RentalOrder.builder()
                .orderId(10L)
                .startDate(LocalDateTime.now().minusDays(5))
                .endDate(LocalDateTime.now())
                .build();
        rentalOrderExtension = RentalOrderExtension.builder()
                .extensionId(100L)
                .rentalOrder(originalOrder)
                .extensionStart(LocalDateTime.now())
                .extensionEnd(LocalDateTime.now().plusDays(3))
                .durationDays(3)
                .pricePerDay(java.math.BigDecimal.valueOf(333))
                .additionalPrice(java.math.BigDecimal.valueOf(1000))
                .build();
        lenient().when(orderDetailRepository.findByRentalOrder_OrderId(originalOrder.getOrderId()))
                .thenReturn(List.of(OrderDetail.builder()
                        .rentalOrder(originalOrder)
                        .deviceModel(DeviceModel.builder().deviceName("Device X").build())
                        .quantity(1L)
                        .build()));
        lenient().when(annexRepository.save(any(ContractExtensionAnnex.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createAnnexForExtensionBuildsAnnexWithNumberAndContent() {
        when(annexRepository.countByContract_ContractId(contract.getContractId())).thenReturn(0L);

        ContractExtensionAnnex annex = service.createAnnexForExtension(contract, rentalOrderExtension, 99L);

        assertThat(annex.getAnnexNumber()).contains("HD202401010001-P");
        assertThat(annex.getExtensionDays()).isEqualTo(3);
        verify(annexRepository).save(any(ContractExtensionAnnex.class));
    }

    @Test
    void createAnnexThrowsWhenMissingData() {
        assertThrows(IllegalArgumentException.class, () -> service.createAnnexForExtension(null, rentalOrderExtension, 1L));
    }

    @Test
    void createAnnexIncrementsSequenceNumber() {
        when(annexRepository.countByContract_ContractId(contract.getContractId())).thenReturn(2L); // existing 2 annexes

        ContractExtensionAnnex annex = service.createAnnexForExtension(contract, rentalOrderExtension, 50L);

        assertThat(annex.getAnnexNumber()).endsWith("-PL-03");
        verify(annexRepository).save(any(ContractExtensionAnnex.class));
    }

    @Test
    void signAsCustomerActivatesAnnexAndCreatesInvoice() throws Exception {
        ContractExtensionAnnex annex = ContractExtensionAnnex.builder()
                .annexId(5L)
                .annexNumber("HD-PL-01")
                .contract(contract)
                .rentalOrderExtension(rentalOrderExtension)
                .status(ContractStatus.PENDING_SIGNATURE)
                .adminSignedAt(LocalDateTime.now())
                .annexContent("content")
                .build();
        when(annexRepository.findById(annex.getAnnexId())).thenReturn(Optional.of(annex));
        when(digitalSignatureService.verifySignature(any(), any(), any())).thenReturn(true);

        seedPinCache(annex.getAnnexId(), "654321");

        ContractExtensionAnnexSignRequestDto request = new ContractExtensionAnnexSignRequestDto();
        request.setDigitalSignature("c2ln");
        request.setSignatureMethod("SMS_OTP");
        request.setPinCode("654321");

        ContractExtensionAnnexResponseDto response = service.signAsCustomer(contract.getContractId(), annex.getAnnexId(), 200L, request);

        assertThat(response.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(annex.getCustomerSignedBy()).isEqualTo(200L);
        assertThat(annex.getInvoice()).isNotNull();
        verify(annexRepository).save(annex);
    }

    @Test
    void signAsCustomerThrowsWhenStatusNotPendingSignature() throws Exception {
        ContractExtensionAnnex annex = ContractExtensionAnnex.builder()
                .annexId(9L)
                .annexNumber("HD-PL-02")
                .contract(contract)
                .rentalOrderExtension(rentalOrderExtension)
                .status(ContractStatus.PENDING_ADMIN_SIGNATURE) // wrong status
                .adminSignedAt(LocalDateTime.now())
                .annexContent("content")
                .build();
        when(annexRepository.findById(annex.getAnnexId())).thenReturn(Optional.of(annex));

        ContractExtensionAnnexSignRequestDto request = new ContractExtensionAnnexSignRequestDto();
        request.setDigitalSignature("c2ln");
        request.setSignatureMethod("SMS_OTP");
        request.setPinCode("654321");

        assertThrows(IllegalStateException.class,
                () -> service.signAsCustomer(contract.getContractId(), annex.getAnnexId(), 200L, request));
    }

    @Test
    void signAsCustomerThrowsWhenAdminNotSigned() throws Exception {
        ContractExtensionAnnex annex = ContractExtensionAnnex.builder()
                .annexId(10L)
                .annexNumber("HD-PL-03")
                .contract(contract)
                .rentalOrderExtension(rentalOrderExtension)
                .status(ContractStatus.PENDING_SIGNATURE)
                .adminSignedAt(null) // admin not signed
                .annexContent("content")
                .build();
        when(annexRepository.findById(annex.getAnnexId())).thenReturn(Optional.of(annex));

        ContractExtensionAnnexSignRequestDto request = new ContractExtensionAnnexSignRequestDto();
        request.setDigitalSignature("c2ln");
        request.setSignatureMethod("SMS_OTP");
        request.setPinCode("654321");

        assertThrows(IllegalStateException.class,
                () -> service.signAsCustomer(contract.getContractId(), annex.getAnnexId(), 200L, request));
    }

    @Test
    void signAsCustomerThrowsWhenPinMissing() throws Exception {
        ContractExtensionAnnex annex = ContractExtensionAnnex.builder()
                .annexId(11L)
                .annexNumber("HD-PL-04")
                .contract(contract)
                .rentalOrderExtension(rentalOrderExtension)
                .status(ContractStatus.PENDING_SIGNATURE)
                .adminSignedAt(LocalDateTime.now())
                .annexContent("content")
                .build();
        when(annexRepository.findById(annex.getAnnexId())).thenReturn(Optional.of(annex));

        ContractExtensionAnnexSignRequestDto request = new ContractExtensionAnnexSignRequestDto();
        request.setDigitalSignature("c2ln");
        request.setSignatureMethod("SMS_OTP");
        request.setPinCode(" "); // missing

        assertThrows(IllegalArgumentException.class,
                () -> service.signAsCustomer(contract.getContractId(), annex.getAnnexId(), 200L, request));
    }

    @Test
    void signAsCustomerThrowsWhenPinInvalid() throws Exception {
        ContractExtensionAnnex annex = ContractExtensionAnnex.builder()
                .annexId(12L)
                .annexNumber("HD-PL-05")
                .contract(contract)
                .rentalOrderExtension(rentalOrderExtension)
                .status(ContractStatus.PENDING_SIGNATURE)
                .adminSignedAt(LocalDateTime.now())
                .annexContent("content")
                .build();
        when(annexRepository.findById(annex.getAnnexId())).thenReturn(Optional.of(annex));
        // Do not seed the PIN cache so validation fails

        ContractExtensionAnnexSignRequestDto request = new ContractExtensionAnnexSignRequestDto();
        request.setDigitalSignature("c2ln");
        request.setSignatureMethod("SMS_OTP");
        request.setPinCode("654321");

        assertThrows(IllegalStateException.class,
                () -> service.signAsCustomer(contract.getContractId(), annex.getAnnexId(), 200L, request));
    }

    @Test
    void signAsCustomerThrowsWhenSignatureInvalid() throws Exception {
        ContractExtensionAnnex annex = ContractExtensionAnnex.builder()
                .annexId(13L)
                .annexNumber("HD-PL-06")
                .contract(contract)
                .rentalOrderExtension(rentalOrderExtension)
                .status(ContractStatus.PENDING_SIGNATURE)
                .adminSignedAt(LocalDateTime.now())
                .annexContent("content")
                .build();
        when(annexRepository.findById(annex.getAnnexId())).thenReturn(Optional.of(annex));
        when(digitalSignatureService.verifySignature(any(), any(), any())).thenReturn(false);
        seedPinCache(annex.getAnnexId(), "654321");

        ContractExtensionAnnexSignRequestDto request = new ContractExtensionAnnexSignRequestDto();
        request.setDigitalSignature("c2ln");
        request.setSignatureMethod("SMS_OTP");
        request.setPinCode("654321");

        assertThrows(IllegalStateException.class,
                () -> service.signAsCustomer(contract.getContractId(), annex.getAnnexId(), 200L, request));
    }

    @Test
    void signAsCustomerThrowsWhenAnnexNotFound() {
        when(annexRepository.findById(999L)).thenReturn(Optional.empty());

        ContractExtensionAnnexSignRequestDto request = new ContractExtensionAnnexSignRequestDto();
        request.setDigitalSignature("c2ln");
        request.setSignatureMethod("SMS_OTP");
        request.setPinCode("654321");

        assertThrows(IllegalArgumentException.class,
                () -> service.signAsCustomer(contract.getContractId(), 999L, 200L, request));
    }

    @Test
    void sendSignaturePinByEmailStoresPin() {
        ContractExtensionAnnex annex = ContractExtensionAnnex.builder()
                .annexId(7L)
                .contract(contract)
                .status(ContractStatus.PENDING_SIGNATURE)
                .annexContent("content")
                .rentalOrderExtension(rentalOrderExtension)
                .build();
        when(annexRepository.findById(annex.getAnnexId())).thenReturn(Optional.of(annex));
        when(emailService.sendOTP(any(), any())).thenReturn(true);

        service.sendSignaturePinByEmail(contract.getContractId(), annex.getAnnexId(), "user@test.com");

        ConcurrentHashMap<Long, ?> cache = getPinCache();
        assertThat(cache).containsKey(annex.getAnnexId());
    }

    @Test
    void sendSignaturePinByEmailRejectsBlankEmail() {
        ContractExtensionAnnex annex = ContractExtensionAnnex.builder()
                .annexId(8L)
                .contract(contract)
                .status(ContractStatus.PENDING_SIGNATURE)
                .annexContent("content")
                .rentalOrderExtension(rentalOrderExtension)
                .build();
        when(annexRepository.findById(annex.getAnnexId())).thenReturn(Optional.of(annex));

        assertThrows(IllegalArgumentException.class,
                () -> service.sendSignaturePinByEmail(contract.getContractId(), annex.getAnnexId(), " "));
    }

    @Test
    void sendSignaturePinByEmailThrowsWhenEmailSendFails() {
        ContractExtensionAnnex annex = ContractExtensionAnnex.builder()
                .annexId(14L)
                .contract(contract)
                .status(ContractStatus.PENDING_SIGNATURE)
                .annexContent("content")
                .rentalOrderExtension(rentalOrderExtension)
                .build();
        when(annexRepository.findById(annex.getAnnexId())).thenReturn(Optional.of(annex));
        when(emailService.sendOTP(any(), any())).thenReturn(false);

        assertThrows(IllegalStateException.class,
                () -> service.sendSignaturePinByEmail(contract.getContractId(), annex.getAnnexId(), "user@test.com"));
    }

    @Test
    void sendSignaturePinByEmailThrowsWhenAnnexDoesNotBelongToContract() {
        Contract otherContract = Contract.builder().contractId(99L).contractNumber("HD-OTHER").build();
        ContractExtensionAnnex annex = ContractExtensionAnnex.builder()
                .annexId(15L)
                .contract(otherContract) // mismatched contract
                .status(ContractStatus.PENDING_SIGNATURE)
                .annexContent("content")
                .rentalOrderExtension(rentalOrderExtension)
                .build();
        when(annexRepository.findById(annex.getAnnexId())).thenReturn(Optional.of(annex));
        when(emailService.sendOTP(any(), any())).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.sendSignaturePinByEmail(contract.getContractId(), annex.getAnnexId(), "user@test.com"));
    }

    private void seedPinCache(Long annexId, String pin) throws Exception {
        ConcurrentHashMap<Long, Object> cache = getPinCache();
        Class<?> entryClass = Class.forName("com.rentaltech.techrental.contract.service.impl.ContractExtensionAnnexServiceImpl$PinCacheEntry");
        Constructor<?> constructor = entryClass.getDeclaredConstructor(String.class, long.class);
        constructor.setAccessible(true);
        Object entry = constructor.newInstance(pin, System.currentTimeMillis() + 60_000);
        cache.put(annexId, entry);
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<Long, Object> getPinCache() {
        return (ConcurrentHashMap<Long, Object>) ReflectionTestUtils.getField(service, "pinCache");
    }
}
