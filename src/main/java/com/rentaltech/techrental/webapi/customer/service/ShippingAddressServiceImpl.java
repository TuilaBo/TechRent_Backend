package com.rentaltech.techrental.webapi.customer.service;

import com.rentaltech.techrental.webapi.customer.model.BankInformation;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.ShippingAddress;
import com.rentaltech.techrental.webapi.customer.model.dto.ShippingAddressRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.ShippingAddressResponseDto;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import com.rentaltech.techrental.webapi.customer.repository.ShippingAddressRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class ShippingAddressServiceImpl implements ShippingAddressService {

    private final ShippingAddressRepository repository;
    private final CustomerRepository customerRepository;

    public ShippingAddressServiceImpl(ShippingAddressRepository repository,
                                      CustomerRepository customerRepository) {
        this.repository = repository;
        this.customerRepository = customerRepository;
    }

    @Override
    public ShippingAddressResponseDto create(ShippingAddressRequestDto request) {
        if (request == null) throw new IllegalArgumentException("ShippingAddressRequestDto is null");
        if (request.getAddress() == null || request.getAddress().isBlank()) {
            throw new IllegalArgumentException("address is required");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Customer customer = customerRepository.findByAccount_Username(auth.getName())
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + auth.getName()));
        if (repository.existsByAddressAndCustomer_CustomerId(request.getAddress(), customer.getCustomerId())) {
            throw new IllegalArgumentException("Address is already exists");
        }

        ShippingAddress entity = ShippingAddress.builder()
                .address(request.getAddress())
                .customer(customer)
                .build();
        ShippingAddress saved = repository.save(entity);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingAddressResponseDto findById(Long id) {
        ShippingAddress entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ShippingAddress not found: " + id));
        enforceOwnershipForCustomer(entity.getCustomer() != null ? entity.getCustomer().getCustomerId() : null);
        return mapToDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingAddressResponseDto> findAll() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isCustomer = auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch("ROLE_CUSTOMER"::equals);
        if (isCustomer) {
            var username = auth.getName();
            var customerOpt = customerRepository.findByAccount_Username(username);
            Long requesterCustomerId = customerOpt.map(Customer::getCustomerId).orElse(-1L);
            return repository.findByCustomer_CustomerId(requesterCustomerId).stream().map(this::mapToDto).toList();
        }
        return repository.findAll().stream().map(this::mapToDto).toList();
    }

    @Override
    public ShippingAddressResponseDto update(Long id, ShippingAddressRequestDto request) {
        ShippingAddress existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ShippingAddress not found: " + id));
        enforceOwnershipForCustomer(existing.getCustomer() != null ? existing.getCustomer().getCustomerId() : null);

        if (request == null) throw new IllegalArgumentException("ShippingAddressRequestDto is null");
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            existing.setAddress(request.getAddress());
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Customer customer = customerRepository.findByAccount_Username(auth.getName())
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + auth.getName()));
        if (repository.existsByAddressAndCustomer_CustomerId(request.getAddress(), customer.getCustomerId())) {
            throw new IllegalArgumentException("Address is already exists");
        }

        ShippingAddress saved = repository.save(existing);
        return mapToDto(saved);
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("ShippingAddress not found: " + id);
        }
        repository.deleteById(id);
    }

    private void enforceOwnershipForCustomer(Long ownerCustomerId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            boolean isCustomer = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_CUSTOMER"::equals);
            if (isCustomer) {
                var username = auth.getName();
                var customerOpt = customerRepository.findByAccount_Username(username);
                Long requesterCustomerId = customerOpt.map(Customer::getCustomerId).orElse(-1L);
                if (ownerCustomerId == null || !ownerCustomerId.equals(requesterCustomerId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: not your shipping address");
                }
            }
        }
    }

    private ShippingAddressResponseDto mapToDto(ShippingAddress entity) {
        return ShippingAddressResponseDto.builder()
                .shippingAddressId(entity.getShippingAddressId())
                .address(entity.getAddress())
                .customerId(entity.getCustomer() != null ? entity.getCustomer().getCustomerId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

