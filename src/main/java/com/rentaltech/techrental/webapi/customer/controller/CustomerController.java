package com.rentaltech.techrental.webapi.customer.controller;

import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerCreateRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerResponseDto;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerUpdateRequestDto;
import com.rentaltech.techrental.webapi.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer")
@AllArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        List<CustomerResponseDto> responseDtos = customers.stream()
                .map(this::mapToResponseDto)
                .toList();
        return ResponseEntity.ok(responseDtos);
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponseDto> getCustomerById(@PathVariable Long customerId) {
        Optional<Customer> customer = customerService.getCustomerById(customerId);
        return customer.map(c -> ResponseEntity.ok(mapToResponseDto(c)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/profile")
    public ResponseEntity<CustomerResponseDto> getMyProfile(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<Customer> customer = customerService.getCustomerByUsername(principal.getUsername());
        return customer.map(c -> ResponseEntity.ok(mapToResponseDto(c)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }



    @PutMapping("/profile")
    public ResponseEntity<CustomerResponseDto> updateMyProfile(
            @RequestBody @Valid CustomerUpdateRequestDto request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            Long accountId = getAccountIdFromPrincipal(principal);
            Customer customer = customerService.updateCustomerByAccountId(accountId, request);
            return ResponseEntity.ok(mapToResponseDto(customer));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerResponseDto> updateCustomer(
            @PathVariable Long customerId,
            @RequestBody @Valid CustomerUpdateRequestDto request) {
        try {
            Customer customer = customerService.updateCustomer(customerId, request);
            return ResponseEntity.ok(mapToResponseDto(customer));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long customerId) {
        try {
            customerService.deleteCustomer(customerId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Long getAccountIdFromPrincipal(org.springframework.security.core.userdetails.UserDetails principal) {
        // Lấy username từ principal và tìm accountId từ AccountService
        String username = principal.getUsername();
        return accountService.getByUsername(username)
                .map(account -> account.getAccountId())
                .orElse(null);
    }

    private CustomerResponseDto mapToResponseDto(Customer customer) {
        return CustomerResponseDto.builder()
                .customerId(customer.getCustomerId())
                .accountId(customer.getAccount().getAccountId())
                .username(customer.getAccount().getUsername())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .fullName(customer.getFullName())
                .shippingAddress(customer.getShippingAddress())
                .bankAccountNumber(customer.getBankAccountNumber())
                .bankName(customer.getBankName())
                .bankAccountHolder(customer.getBankAccountHolder())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
