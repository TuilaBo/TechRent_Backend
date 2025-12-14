package com.rentaltech.techrental.webapi.customer.service;

import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.ShippingAddress;
import com.rentaltech.techrental.webapi.customer.model.dto.ShippingAddressRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.ShippingAddressResponseDto;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import com.rentaltech.techrental.webapi.customer.repository.ShippingAddressRepository;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingAddressServiceImplTest {

    @Mock
    private ShippingAddressRepository shippingAddressRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private ShippingAddressServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRejectsDuplicateAddress() {
        ShippingAddressRequestDto request = ShippingAddressRequestDto.builder()
                .address("123 Street")
                .build();
        Customer customer = customer(1L);

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("user");
        when(customerRepository.findByAccount_Username("user")).thenReturn(Optional.of(customer));
        when(shippingAddressRepository.existsByAddressAndCustomer_CustomerId("123 Street", 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Địa chỉ đã tồn tại");
        verify(shippingAddressRepository, never()).save(any());
    }

    @Test
    void createPersistsWhenValid() {
        ShippingAddressRequestDto request = ShippingAddressRequestDto.builder()
                .address("456 Street")
                .build();
        Customer customer = customer(2L);

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("user");
        when(customerRepository.findByAccount_Username("user")).thenReturn(Optional.of(customer));
        when(shippingAddressRepository.existsByAddressAndCustomer_CustomerId("456 Street", 2L))
                .thenReturn(false);
        when(shippingAddressRepository.save(any(ShippingAddress.class))).thenAnswer(invocation -> {
            ShippingAddress entity = invocation.getArgument(0);
            entity.setShippingAddressId(15L);
            return entity;
        });

        ShippingAddressResponseDto dto = service.create(request);

        assertThat(dto.getShippingAddressId()).isEqualTo(15L);
        assertThat(dto.getAddress()).isEqualTo("456 Street");
        verify(shippingAddressRepository).save(any(ShippingAddress.class));
    }

    @Test
    void findByIdEnforcesOwnership() {
        Customer owner = customer(3L);
        ShippingAddress entity = ShippingAddress.builder()
                .shippingAddressId(20L)
                .customer(owner)
                .build();

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("user");
        when(authentication.isAuthenticated()).thenReturn(true);
        Collection<GrantedAuthority> authorities = List.of((GrantedAuthority) () -> "ROLE_CUSTOMER");
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);  // use thenAnswer so no generic mismatch
        when(shippingAddressRepository.findById(20L)).thenReturn(Optional.of(entity));
        when(customerRepository.findByAccount_Username("user")).thenReturn(Optional.of(customer(4L)));

        assertThatThrownBy(() -> service.findById(20L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("không thuộc về bạn");
    }

    @Test
    void findAllReturnsOnlyCustomerAddresses() {
        Customer owner = customer(5L);
        ShippingAddress entity = ShippingAddress.builder()
                .shippingAddressId(9L)
                .customer(owner)
                .address("A")
                .build();

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        Collection<GrantedAuthority> authorities = List.of((GrantedAuthority) () -> "ROLE_CUSTOMER");
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);  // use thenAnswer so no generic mismatch
        when(authentication.getName()).thenReturn("user");
        when(customerRepository.findByAccount_Username("user")).thenReturn(Optional.of(owner));
        when(shippingAddressRepository.findByCustomer_CustomerId(5L)).thenReturn(List.of(entity));

        List<ShippingAddressResponseDto> dtos = service.findAll();

        assertThat(dtos).hasSize(1);
        verify(shippingAddressRepository, never()).findAll();
    }

    @Test
    void updateRejectsDuplicateAddress() {
        Customer owner = customer(6L);
        ShippingAddress entity = ShippingAddress.builder()
                .shippingAddressId(11L)
                .customer(owner)
                .address("old")
                .build();
        ShippingAddressRequestDto request = ShippingAddressRequestDto.builder()
                .address("new")
                .build();

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("user");
        when(authentication.isAuthenticated()).thenReturn(true);
        Collection<GrantedAuthority> authorities = List.of((GrantedAuthority) () -> "ROLE_CUSTOMER");
        when(authentication.getAuthorities()).thenAnswer(inv -> authorities);  // use thenAnswer so no generic mismatch
        when(shippingAddressRepository.findById(11L)).thenReturn(Optional.of(entity));
        when(customerRepository.findByAccount_Username("user")).thenReturn(Optional.of(owner));
        when(shippingAddressRepository.existsByAddressAndCustomer_CustomerId("new", 6L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(11L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Địa chỉ đã tồn tại");
        verify(shippingAddressRepository, never()).save(any());
    }

    @Test
    void deleteValidatesExistence() {
        when(shippingAddressRepository.existsById(77L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(77L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Không tìm thấy");
    }

    private Customer customer(Long id) {
        return Customer.builder()
                .customerId(id)
                .build();
    }
}
