package com.rentaltech.techrental.contract.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.contract.model.Contract;
import com.rentaltech.techrental.contract.model.ContractStatus;
import com.rentaltech.techrental.contract.model.ContractType;
import com.rentaltech.techrental.contract.model.dto.ContractCreateRequestDto;
import com.rentaltech.techrental.contract.model.dto.DigitalSignatureRequestDto;
import com.rentaltech.techrental.contract.model.dto.DigitalSignatureResponseDto;
import com.rentaltech.techrental.contract.repository.ContractRepository;
import com.rentaltech.techrental.contract.service.impl.ContractServiceImpl;
import com.rentaltech.techrental.device.model.Brand;
import com.rentaltech.techrental.device.model.DeviceCategory;
import com.rentaltech.techrental.device.model.DeviceModel;
import com.rentaltech.techrental.rentalorder.model.OrderDetail;
import com.rentaltech.techrental.rentalorder.model.OrderStatus;
import com.rentaltech.techrental.rentalorder.model.RentalOrder;
import com.rentaltech.techrental.rentalorder.repository.OrderDetailRepository;
import com.rentaltech.techrental.rentalorder.repository.RentalOrderRepository;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import com.rentaltech.techrental.webapi.customer.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceImplTest {

    @InjectMocks
    private ContractServiceImpl contractService;

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private SMSService smsService;
    @Mock
    private EmailService emailService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private DigitalSignatureService digitalSignatureService;
    @Mock
    private RentalOrderRepository rentalOrderRepository;
    @Mock
    private OrderDetailRepository orderDetailRepository;
    @Mock
    private DeviceContractTermService deviceContractTermService;
    @Mock
    private ContractExtensionAnnexService contractExtensionAnnexService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private CustomerRepository customerRepository;

    @Test
    void sendSmsPinReturnsNotFoundWhenContractMissing() {
        when(contractRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = contractService.sendSMSPIN(1L, "0901234567");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void sendSmsPinSendsCodeWhenContractIsPendingSignature() {
        Contract contract = baseContract(ContractStatus.PENDING_SIGNATURE);
        when(contractRepository.findById(contract.getContractId())).thenReturn(Optional.of(contract));
        when(smsService.sendOTP(anyString(), anyString())).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ResponseEntity<?> response = contractService.sendSMSPIN(contract.getContractId(), "0901234567");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(smsService).sendOTP(eq("0901234567"), anyString());
        verify(valueOperations).set(startsWith("contract_pin_"), anyString(), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    void sendSmsPinReturnsErrorWhenSmsFails() {
        Contract contract = baseContract(ContractStatus.PENDING_SIGNATURE);
        when(contractRepository.findById(contract.getContractId())).thenReturn(Optional.of(contract));
        when(smsService.sendOTP(anyString(), anyString())).thenReturn(false);

        ResponseEntity<?> response = contractService.sendSMSPIN(contract.getContractId(), "0901234567");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void sendEmailPinReturnsOkWhenEmailSent() {
        when(emailService.sendOTP(anyString(), anyString())).thenReturn(true);

        ResponseEntity<?> response = contractService.sendEmailPIN(10L, "user@test.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(emailService).sendOTP(eq("user@test.com"), anyString());
    }

    @Test
    void sendEmailPinReturnsErrorWhenEmailFails() {
        when(emailService.sendOTP(anyString(), anyString())).thenReturn(false);

        ResponseEntity<?> response = contractService.sendEmailPIN(10L, "user@test.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void sendEmailPinStoresPinInRedisOnSuccess() {
        when(emailService.sendOTP(anyString(), anyString())).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ResponseEntity<?> response = contractService.sendEmailPIN(22L, "user@test.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(valueOperations).set(startsWith("contract_pin_"), anyString(), eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    void sendEmailPinReturnsErrorWhenExceptionOccurs() {
        when(emailService.sendOTP(anyString(), anyString())).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = contractService.sendEmailPIN(33L, "user@test.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void signContractPersistsCustomerSignature() {
        long contractId = 5L;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("contract_pin_" + contractId)).thenReturn("123456");
        Contract contract = baseContract(ContractStatus.PENDING_SIGNATURE);
        contract.setContractId(contractId);
        contract.setAdminSignedAt(LocalDateTime.now().minusDays(1));
        contract.setAdminSignedBy(999L);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(digitalSignatureService.verifySignature(anyString(), anyString(), anyString())).thenReturn(true);
        when(redisTemplate.delete("contract_pin_" + contractId)).thenReturn(true);

        DigitalSignatureRequestDto request = DigitalSignatureRequestDto.builder()
                .contractId(contractId)
                .digitalSignature("c2ln")
                .pinCode("123456")
                .signatureMethod("SMS_OTP")
                .deviceInfo("Chrome")
                .ipAddress("127.0.0.1")
                .build();

        DigitalSignatureResponseDto response = contractService.signContract(request);

        assertThat(response.getContractId()).isEqualTo(contractId);
        assertThat(contract.getCustomerSignedAt()).isNotNull();
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        verify(contractRepository).save(contract);
        verify(redisTemplate).delete("contract_pin_" + contractId);
    }

    @Test
    void signContractThrowsWhenPinInvalid() {
        long contractId = 6L;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // Mismatch PIN
        when(valueOperations.get("contract_pin_" + contractId)).thenReturn("000000");

        DigitalSignatureRequestDto request = DigitalSignatureRequestDto.builder()
                .contractId(contractId)
                .pinCode("123456")
                .build();

        assertThatThrownBy(() -> contractService.signContract(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("PIN code");
    }

    @Test
    void signContractThrowsWhenContractNotFound() {
        long contractId = 7L;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("contract_pin_" + contractId)).thenReturn("123456");
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

        DigitalSignatureRequestDto request = DigitalSignatureRequestDto.builder()
                .contractId(contractId)
                .pinCode("123456")
                .build();

        assertThatThrownBy(() -> contractService.signContract(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy hợp đồng");
    }

    @Test
    void signContractThrowsWhenStatusNotPendingSignature() {
        long contractId = 8L;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("contract_pin_" + contractId)).thenReturn("123456");
        Contract contract = baseContract(ContractStatus.DRAFT);
        contract.setContractId(contractId);
        // Admin đã ký nhưng trạng thái không đúng
        contract.setAdminSignedAt(LocalDateTime.now());
        contract.setAdminSignedBy(1L);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        DigitalSignatureRequestDto request = DigitalSignatureRequestDto.builder()
                .contractId(contractId)
                .pinCode("123456")
                .digitalSignature("c2ln")
                .signatureMethod("SMS_OTP")
                .build();

        assertThatThrownBy(() -> contractService.signContract(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("chưa sẵn sàng");
    }

    @Test
    void signContractThrowsWhenAdminNotSigned() {
        long contractId = 9L;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("contract_pin_" + contractId)).thenReturn("123456");
        Contract contract = baseContract(ContractStatus.PENDING_SIGNATURE);
        contract.setContractId(contractId);
        // Admin chưa ký
        contract.setAdminSignedAt(null);
        contract.setAdminSignedBy(null);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        DigitalSignatureRequestDto request = DigitalSignatureRequestDto.builder()
                .contractId(contractId)
                .pinCode("123456")
                .digitalSignature("c2ln")
                .signatureMethod("SMS_OTP")
                .build();

        assertThatThrownBy(() -> contractService.signContract(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Admin chưa ký");
    }

    @Test
    void signContractThrowsWhenSignatureInvalid() {
        long contractId = 10L;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("contract_pin_" + contractId)).thenReturn("123456");
        Contract contract = baseContract(ContractStatus.PENDING_SIGNATURE);
        contract.setContractId(contractId);
        contract.setAdminSignedAt(LocalDateTime.now());
        contract.setAdminSignedBy(1L);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(digitalSignatureService.verifySignature(anyString(), anyString(), anyString())).thenReturn(false);

        DigitalSignatureRequestDto request = DigitalSignatureRequestDto.builder()
                .contractId(contractId)
                .pinCode("123456")
                .digitalSignature("c2ln")
                .signatureMethod("SMS_OTP")
                .build();

        assertThatThrownBy(() -> contractService.signContract(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("không hợp lệ");
    }

    @Test
    void signContractByAdminMovesContractToPendingCustomerSignature() {
        long contractId = 2L;
        Contract contract = baseContract(ContractStatus.PENDING_ADMIN_SIGNATURE);
        contract.setContractId(contractId);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(digitalSignatureService.verifySignature(anyString(), anyString(), anyString())).thenReturn(true);

        DigitalSignatureRequestDto request = DigitalSignatureRequestDto.builder()
                .digitalSignature("c2ln")
                .signatureMethod("ADMIN")
                .deviceInfo("Chrome")
                .ipAddress("127.0.0.1")
                .build();

        DigitalSignatureResponseDto response = contractService.signContractByAdmin(contractId, 100L, request);

        assertThat(response.getContractId()).isEqualTo(contractId);
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.PENDING_SIGNATURE);
        assertThat(contract.getAdminSignedBy()).isEqualTo(100L);
        verify(contractRepository).save(contract);
    }

    @Test
    void signContractByAdminThrowsWhenContractNotFound() {
        long contractId = 404L;
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());

        DigitalSignatureRequestDto request = DigitalSignatureRequestDto.builder()
                .digitalSignature("c2ln")
                .build();

        assertThatThrownBy(() -> contractService.signContractByAdmin(contractId, 100L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy hợp đồng");
    }

    @Test
    void signContractByAdminThrowsWhenWrongStatus() {
        long contractId = 3L;
        Contract contract = baseContract(ContractStatus.DRAFT);
        contract.setContractId(contractId);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        DigitalSignatureRequestDto request = DigitalSignatureRequestDto.builder()
                .digitalSignature("c2ln")
                .signatureMethod("ADMIN")
                .build();

        assertThatThrownBy(() -> contractService.signContractByAdmin(contractId, 100L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("không ở trạng thái chờ admin ký");
    }

    @Test
    void signContractByAdminThrowsWhenSignatureInvalid() {
        long contractId = 12L;
        Contract contract = baseContract(ContractStatus.PENDING_ADMIN_SIGNATURE);
        contract.setContractId(contractId);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(digitalSignatureService.verifySignature(anyString(), anyString(), anyString())).thenReturn(false);

        DigitalSignatureRequestDto request = DigitalSignatureRequestDto.builder()
                .digitalSignature("c2ln")
                .signatureMethod("ADMIN")
                .build();

        assertThatThrownBy(() -> contractService.signContractByAdmin(contractId, 100L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("không hợp lệ");
    }

    @Test
    void getAllContractsDelegatesToRepository() {
        List<Contract> contracts = List.of(baseContract(ContractStatus.DRAFT));
        when(contractRepository.findAll()).thenReturn(contracts);

        assertThat(contractService.getAllContracts()).isEqualTo(contracts);
    }

    @Test
    void getContractLookupsReturnRepositoryValues() {
        Contract contract = baseContract(ContractStatus.DRAFT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(contractRepository.findByContractNumber("HD1")).thenReturn(Optional.of(contract));
        when(contractRepository.findByCustomerId(2L)).thenReturn(List.of(contract));
        when(contractRepository.findByStatus(ContractStatus.DRAFT)).thenReturn(List.of(contract));
        when(contractRepository.findByStatusAndCustomerId(ContractStatus.DRAFT, 2L)).thenReturn(List.of(contract));

        assertThat(contractService.getContractById(1L)).contains(contract);
        assertThat(contractService.getContractByNumber("HD1")).contains(contract);
        assertThat(contractService.getContractsByCustomerId(2L)).containsExactly(contract);
        assertThat(contractService.getContractsByStatus(ContractStatus.DRAFT)).containsExactly(contract);
        assertThat(contractService.getContractsByCustomerIdAndStatus(2L, ContractStatus.DRAFT)).containsExactly(contract);
    }

    @Test
    void createContractPersistsGeneratedContract() {
        ContractCreateRequestDto request = buildCreateRequest();
        RentalOrder order = sampleOrder();
        order.setOrderId(request.getOrderId());
        when(rentalOrderRepository.findById(request.getOrderId())).thenReturn(Optional.of(order));
        when(orderDetailRepository.findByRentalOrder_OrderId(request.getOrderId())).thenReturn(sampleOrderDetails(order));

        contractService.createContract(request, 99L);

        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(captor.capture());
        Contract saved = captor.getValue();
        assertThat(saved.getContractNumber()).startsWith("HD");
        assertThat(saved.getCreatedBy()).isEqualTo(99L);
        assertThat(saved.getCustomerId()).isEqualTo(request.getCustomerId());
    }

    @Test
    void createContractWithoutOrderIdSavesWithoutEnrichment() {
        ContractCreateRequestDto request = buildCreateRequest();
        request.setOrderId(null);

        contractService.createContract(request, 77L);

        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(captor.capture());
        Contract saved = captor.getValue();
        assertThat(saved.getOrderId()).isNull();
        assertThat(saved.getCreatedBy()).isEqualTo(77L);
        // No order lookups when orderId is null
        verify(rentalOrderRepository, never()).findById(anyLong());
        verify(orderDetailRepository, never()).findByRentalOrder_OrderId(anyLong());
        verify(deviceContractTermService, never()).findApplicableTerms(any(), any());
    }

    @Test
    void createContractIgnoresMissingOrder() {
        ContractCreateRequestDto request = buildCreateRequest();
        // OrderId provided but not found
        when(rentalOrderRepository.findById(request.getOrderId())).thenReturn(Optional.empty());

        contractService.createContract(request, 11L);

        verify(contractRepository).save(any(Contract.class));
        verify(orderDetailRepository, never()).findByRentalOrder_OrderId(anyLong());
        verify(deviceContractTermService, never()).findApplicableTerms(any(), any());
    }

    @Test
    void createContractEnrichesTermsWhenOrderExists() {
        ContractCreateRequestDto request = buildCreateRequest();
        RentalOrder order = sampleOrder();
        order.setOrderId(request.getOrderId());
        when(rentalOrderRepository.findById(request.getOrderId())).thenReturn(Optional.of(order));
        List<OrderDetail> details = sampleOrderDetails(order);
        when(orderDetailRepository.findByRentalOrder_OrderId(request.getOrderId())).thenReturn(details);
        // Return empty list to keep content simple but verify method call
        when(deviceContractTermService.findApplicableTerms(order, details)).thenReturn(Collections.emptyList());

        contractService.createContract(request, 22L);

        verify(orderDetailRepository).findByRentalOrder_OrderId(request.getOrderId());
        verify(deviceContractTermService).findApplicableTerms(order, details);
        verify(contractRepository).save(any(Contract.class));
    }

    @Test
    void createContractThrowsWhenSaveFails() {
        ContractCreateRequestDto request = buildCreateRequest();
        request.setOrderId(null); // avoid order lookups
        when(contractRepository.save(any(Contract.class))).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> contractService.createContract(request, 33L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không thể tạo hợp đồng");
    }

    @Test
    void updateContractCurrentlyReturnsNull() {
        assertNull(contractService.updateContract(1L, buildCreateRequest(), 2L));
    }

    @Test
    void createContractFromOrderBuildsNewContract() {
        RentalOrder order = sampleOrder();
        order.setOrderId(10L);
        when(rentalOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderDetailRepository.findByRentalOrder_OrderId(10L)).thenReturn(sampleOrderDetails(order));
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

        Contract created = contractService.createContractFromOrder(10L, 50L);

        assertThat(created.getOrderId()).isEqualTo(10L);
        assertThat(created.getStatus()).isEqualTo(ContractStatus.DRAFT);
    }

    @Test
    void createContractFromOrderInvokesTermEnrichment() {
        long orderId = 55L;
        RentalOrder order = sampleOrder();
        order.setOrderId(orderId);
        when(rentalOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        List<OrderDetail> details = sampleOrderDetails(order);
        when(orderDetailRepository.findByRentalOrder_OrderId(orderId)).thenReturn(details);
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

        contractService.createContractFromOrder(orderId, 77L);

        verify(deviceContractTermService).findApplicableTerms(order, details);
    }

    @Test
    void createContractFromOrderHandlesEmptyOrderDetails() {
        long orderId = 66L;
        RentalOrder order = sampleOrder();
        order.setOrderId(orderId);
        when(rentalOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderDetailRepository.findByRentalOrder_OrderId(orderId)).thenReturn(Collections.emptyList());
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

        Contract created = contractService.createContractFromOrder(orderId, 88L);

        assertThat(created.getOrderId()).isEqualTo(orderId);
        assertThat(created.getStatus()).isEqualTo(ContractStatus.DRAFT);
    }

    @Test
    void createContractFromOrderThrowsWhenOrderMissing() {
        when(rentalOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contractService.createContractFromOrder(999L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không thể tạo hợp đồng từ đơn thuê");
    }

    @Test
    void createContractFromOrderThrowsWhenSaveFails() {
        long orderId = 77L;
        RentalOrder order = sampleOrder();
        order.setOrderId(orderId);
        when(rentalOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderDetailRepository.findByRentalOrder_OrderId(orderId)).thenReturn(sampleOrderDetails(order));
        when(contractRepository.save(any(Contract.class))).thenThrow(new RuntimeException("db error"));

        assertThatThrownBy(() -> contractService.createContractFromOrder(orderId, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không thể tạo hợp đồng từ đơn thuê");
    }

    @Test
    void deleteContractDoesNothingForNow() {
        contractService.deleteContract(99L);
    }

    @Test
    void updateContractStatusPersistsNewStatus() {
        Contract contract = baseContract(ContractStatus.DRAFT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(contractRepository.save(contract)).thenAnswer(inv -> inv.getArgument(0));

        Contract updated = contractService.updateContractStatus(1L, ContractStatus.ACTIVE);

        assertThat(updated.getStatus()).isEqualTo(ContractStatus.ACTIVE);
        verify(contractRepository).save(contract);
    }

    @Test
    void updateContractStatusThrowsWhenNotFound() {
        when(contractRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contractService.updateContractStatus(123L, ContractStatus.ACTIVE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không thể cập nhật trạng thái hợp đồng");
    }

    @Test
    void updateContractStatusThrowsWhenSaveFails() {
        Contract contract = baseContract(ContractStatus.DRAFT);
        when(contractRepository.findById(2L)).thenReturn(Optional.of(contract));
        when(contractRepository.save(contract)).thenThrow(new RuntimeException("db error"));

        assertThatThrownBy(() -> contractService.updateContractStatus(2L, ContractStatus.CANCELLED))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không thể cập nhật trạng thái hợp đồng");
    }

    @Test
    void sendForSignatureMovesDraftContractToPendingAdminSignature() {
        Contract contract = baseContract(ContractStatus.DRAFT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(contractRepository.save(contract)).thenAnswer(inv -> inv.getArgument(0));

        Contract updated = contractService.sendForSignature(1L, 200L);

        assertThat(updated.getStatus()).isEqualTo(ContractStatus.PENDING_ADMIN_SIGNATURE);
        verify(contractRepository).save(contract);
    }

    @Test
    void sendForSignatureThrowsWhenNotDraft() {
        Contract contract = baseContract(ContractStatus.ACTIVE);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> contractService.sendForSignature(1L, 200L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void sendForSignatureThrowsWhenContractNotFound() {
        when(contractRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contractService.sendForSignature(404L, 200L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không thể gửi hợp đồng để ký");
    }

    @Test
    void sendForSignatureThrowsWhenSaveFails() {
        Contract contract = baseContract(ContractStatus.DRAFT);
        when(contractRepository.findById(2L)).thenReturn(Optional.of(contract));
        when(contractRepository.save(contract)).thenThrow(new RuntimeException("db error"));

        assertThatThrownBy(() -> contractService.sendForSignature(2L, 200L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không thể gửi hợp đồng để ký");
    }

    @Test
    void cancelContractCurrentlyReturnsNull() {
        assertNull(contractService.cancelContract(1L, "reason", 3L));
    }

    @Test
    void verifySignatureNotImplementedYet() {
        assertThat(contractService.verifySignature(1L)).isFalse();
    }

    @Test
    void signatureInfoNotImplementedYet() {
        assertNull(contractService.getSignatureInfo(1L));
    }

    @Test
    void expiredContractQueriesNotImplementedYet() {
        assertNull(contractService.getExpiredContracts());
        assertNull(contractService.getContractsExpiringSoon(5));
    }

    @Test
    void renewContractNotImplementedYet() {
        assertNull(contractService.renewContract(1L, 3));
    }

    @Test
    void validateContractForSignatureRequiresAdminSignature() {
        Contract contract = baseContract(ContractStatus.PENDING_SIGNATURE);
        contract.setAdminSignedAt(LocalDateTime.now());
        contract.setAdminSignedBy(10L);
        contract.setContractContent("content");
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        assertThat(contractService.validateContractForSignature(1L)).isTrue();

        contract.setAdminSignedAt(null);
        assertThat(contractService.validateContractForSignature(1L)).isFalse();
    }

    @Test
    void validateContractForSignatureReturnsFalseWhenWrongStatus() {
        Contract contract = baseContract(ContractStatus.DRAFT);
        contract.setAdminSignedAt(LocalDateTime.now());
        contract.setAdminSignedBy(10L);
        contract.setContractContent("content");
        when(contractRepository.findById(2L)).thenReturn(Optional.of(contract));

        assertThat(contractService.validateContractForSignature(2L)).isFalse();
    }

    @Test
    void validateContractForSignatureReturnsFalseWhenExpired() {
        Contract contract = baseContract(ContractStatus.PENDING_SIGNATURE);
        contract.setAdminSignedAt(LocalDateTime.now());
        contract.setAdminSignedBy(10L);
        contract.setContractContent("content");
        contract.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(contractRepository.findById(3L)).thenReturn(Optional.of(contract));

        assertThat(contractService.validateContractForSignature(3L)).isFalse();
    }

    @Test
    void validateContractForSignatureReturnsFalseWhenNotFound() {
        when(contractRepository.findById(404L)).thenReturn(Optional.empty());

        assertThat(contractService.validateContractForSignature(404L)).isFalse();
    }

    @Test
    void validateSignatureDataChecksRequiredFields() {
        DigitalSignatureRequestDto valid = DigitalSignatureRequestDto.builder()
                .contractId(1L)
                .digitalSignature("abc")
                .pinCode("123456")
                .signatureMethod("SMS_OTP")
                .build();

        assertThat(contractService.validateSignatureData(valid)).isTrue();

        valid.setPinCode("12345");
        assertThat(contractService.validateSignatureData(valid)).isFalse();
    }

    @Test
    void validateSignatureDataRejectsNullContractId() {
        DigitalSignatureRequestDto req = DigitalSignatureRequestDto.builder()
                .contractId(null)
                .digitalSignature("abc")
                .pinCode("123456")
                .signatureMethod("SMS_OTP")
                .build();

        assertThat(contractService.validateSignatureData(req)).isFalse();
    }

    @Test
    void validateSignatureDataRejectsEmptySignature() {
        DigitalSignatureRequestDto req = DigitalSignatureRequestDto.builder()
                .contractId(1L)
                .digitalSignature(" ")
                .pinCode("123456")
                .signatureMethod("SMS_OTP")
                .build();

        assertThat(contractService.validateSignatureData(req)).isFalse();
    }

    @Test
    void validateSignatureDataRejectsUnsupportedMethod() {
        DigitalSignatureRequestDto req = DigitalSignatureRequestDto.builder()
                .contractId(1L)
                .digitalSignature("abc")
                .pinCode("123456")
                .signatureMethod("UNSUPPORTED")
                .build();

        assertThat(contractService.validateSignatureData(req)).isFalse();
    }

    @Test
    void generateContractNumberUsesDailySequence() {
        when(contractRepository.findByContractNumberStartingWith(anyString())).thenReturn(List.of(baseContract(ContractStatus.DRAFT)));

        String number = contractService.generateContractNumber();

        assertThat(number).matches("HD\\d{8}\\d{4}");
    }

    @Test
    void generateContractNumberStartsAt0001WhenNoExisting() {
        when(contractRepository.findByContractNumberStartingWith(anyString())).thenReturn(List.of());

        String number = contractService.generateContractNumber();

        String datePrefix = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertThat(number).isEqualTo("HD" + datePrefix + "0001");
    }

    @Test
    void generateContractNumberIncrementsByExistingCount() {
        // Simulate 5 contracts already today -> next should be 0006
        when(contractRepository.findByContractNumberStartingWith(anyString())).thenReturn(
                java.util.Arrays.asList(new Contract(), new Contract(), new Contract(), new Contract(), new Contract())
        );

        String number = contractService.generateContractNumber();

        String datePrefix = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertThat(number).isEqualTo("HD" + datePrefix + "0006");
    }

    @Test
    void generateAuditTrailNotImplementedYet() {
        assertNull(contractService.generateAuditTrail(1L));
    }

    private Contract baseContract(ContractStatus status) {
        return Contract.builder()
                .contractId(1L)
                .contractNumber("HD202401010001")
                .title("Test")
                .description("Desc")
                .contractType(ContractType.EQUIPMENT_RENTAL)
                .status(status)
                .customerId(5L)
                .contractContent("content")
                .createdAt(LocalDateTime.now().minusDays(1))
                .adminSignedAt(LocalDateTime.now().minusDays(2))
                .adminSignedBy(10L)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build();
    }

    private ContractCreateRequestDto buildCreateRequest() {
        ContractCreateRequestDto dto = new ContractCreateRequestDto();
        dto.setTitle("Contract");
        dto.setDescription("Desc");
        dto.setContractType(ContractType.EQUIPMENT_RENTAL);
        dto.setCustomerId(7L);
        dto.setOrderId(20L);
        dto.setContractContent("content");
        dto.setTermsAndConditions("terms");
        dto.setRentalPeriodDays(10);
        dto.setTotalAmount(BigDecimal.valueOf(1000));
        dto.setDepositAmount(BigDecimal.valueOf(100));
        dto.setStartDate(LocalDateTime.now().plusDays(1));
        dto.setEndDate(LocalDateTime.now().plusDays(5));
        dto.setExpiresAt(LocalDateTime.now().plusDays(6));
        return dto;
    }

    private RentalOrder sampleOrder() {
        Customer customer = Customer.builder()
                .customerId(7L)
                .account(Account.builder().accountId(77L).username("customer").role(Role.CUSTOMER).build())
                .build();
        return RentalOrder.builder()
                .orderId(20L)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .orderStatus(OrderStatus.PENDING)
                .depositAmount(BigDecimal.valueOf(100))
                .depositAmountHeld(BigDecimal.ZERO)
                .totalPrice(BigDecimal.valueOf(1000))
                .pricePerDay(BigDecimal.valueOf(100))
                .customer(customer)
                .build();
    }

    private List<OrderDetail> sampleOrderDetails(RentalOrder order) {
        DeviceCategory category = DeviceCategory.builder()
                .deviceCategoryId(1L)
                .deviceCategoryName("Laptop")
                .build();
        DeviceModel model = DeviceModel.builder()
                .deviceModelId(2L)
                .deviceName("Model X")
                .brand(Brand.builder().brandId(3L).brandName("Brand").build())
                .deviceCategory(category)
                .pricePerDay(BigDecimal.TEN)
                .depositPercent(BigDecimal.ONE)
                .deviceValue(BigDecimal.valueOf(1000))
                .build();
        OrderDetail detail = OrderDetail.builder()
                .orderDetailId(1L)
                .rentalOrder(order)
                .deviceModel(model)
                .quantity(1L)
                .pricePerDay(BigDecimal.TEN)
                .depositAmountPerUnit(BigDecimal.ONE)
                .build();
        return List.of(detail);
    }
}
