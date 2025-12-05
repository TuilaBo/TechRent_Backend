package com.rentaltech.techrental.webapi.customer.service;

import com.rentaltech.techrental.webapi.customer.model.BankInformation;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.dto.BankInformationRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.BankInformationResponseDto;
import com.rentaltech.techrental.webapi.customer.repository.BankInformationRepository;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankInformationServiceImplTest {

    @Mock
    private BankInformationRepository bankInformationRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private BankInformationServiceImpl service;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDownSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRejectsDuplicateCardNumberForCustomer() {
        BankInformationRequestDto request = BankInformationRequestDto.builder()
                .bankName("VCB")
                .bankHolder("John Doe")
                .cardNumber("123")
                .build();

        Customer customer = customer(10L);
        when(customerRepository.findByAccount_Username(null)).thenReturn(Optional.of(customer));
        when(bankInformationRepository.existsByCardNumberAndCustomer_CustomerId("123", 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("được sử dụng");

        verify(bankInformationRepository, never()).save(any());
    }

    @Test
    void createPersistsWhenInputValid() {
        BankInformationRequestDto request = BankInformationRequestDto.builder()
                .bankName("VCB")
                .bankHolder("John Doe")
                .cardNumber("123")
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("user");

        Customer customer = customer(11L);
        when(customerRepository.findByAccount_Username("user")).thenReturn(Optional.of(customer));
        when(bankInformationRepository.existsByCardNumberAndCustomer_CustomerId("123", 11L)).thenReturn(false);
        when(bankInformationRepository.save(any(BankInformation.class))).thenAnswer(invocation -> {
            BankInformation entity = invocation.getArgument(0);
            entity.setBankInformationId(5L);
            return entity;
        });

        BankInformationResponseDto dto = service.create(request);

        assertThat(dto.getBankInformationId()).isEqualTo(5L);
        assertThat(dto.getBankHolder()).isEqualTo("John Doe");
        verify(bankInformationRepository).save(any(BankInformation.class));
    }

    @Test
    void findByIdEnforcesOwnershipForCustomerRole() {
        Customer owner = customer(20L);
        BankInformation info = BankInformation.builder()
                .bankInformationId(99L)
                .customer(owner)
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user");
        Collection<GrantedAuthority> authorities = List.of((GrantedAuthority) () -> "ROLE_CUSTOMER");
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);  // use thenAnswer so no generic mismatch

        when(bankInformationRepository.findById(99L)).thenReturn(Optional.of(info));
        when(customerRepository.findByAccount_Username("user")).thenReturn(Optional.of(customer(21L)));

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("không thuộc về bạn");
    }

    @Test
    void findAllReturnsOnlyCustomerRecordsWhenRoleCustomer() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user");
        Collection<GrantedAuthority> authorities = List.of((GrantedAuthority) () -> "ROLE_CUSTOMER");
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);  // use thenAnswer so no generic mismatch


        Customer me = customer(30L);
        when(customerRepository.findByAccount_Username("user")).thenReturn(Optional.of(me));
        BankInformation info = BankInformation.builder()
                .bankInformationId(1L)
                .customer(me)
                .build();
        when(bankInformationRepository.findByCustomer_CustomerId(30L)).thenReturn(List.of(info));

        List<BankInformationResponseDto> result = service.findAll();

        assertThat(result).hasSize(1);
        verify(bankInformationRepository, never()).findAll();
    }

    @Test
    void updateRejectsDuplicateCardForSameOwner() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user");
        Collection<GrantedAuthority> authorities = List.of((GrantedAuthority) () -> "ROLE_CUSTOMER");
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);  // use thenAnswer so no generic mismatch


        Customer me = customer(40L);
        BankInformation existing = BankInformation.builder()
                .bankInformationId(7L)
                .customer(me)
                .build();

        BankInformationRequestDto request = BankInformationRequestDto.builder()
                .cardNumber("999")
                .build();

        when(bankInformationRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(customerRepository.findByAccount_Username("user")).thenReturn(Optional.of(me));
        when(bankInformationRepository.existsByCardNumberAndCustomer_CustomerId("999", 40L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(7L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("được sử dụng");

        verify(bankInformationRepository, never()).save(any());
    }

    @Test
    void deleteValidatesExistence() {
        when(bankInformationRepository.existsById(88L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(88L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Không tìm thấy");
    }

    private Customer customer(Long id) {
        return Customer.builder()
                .customerId(id)
                .build();
    }
}
