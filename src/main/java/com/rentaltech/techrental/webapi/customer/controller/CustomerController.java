package com.rentaltech.techrental.webapi.customer.controller;

import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.dto.BankInformationResponseDto;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerResponseDto;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerUpdateRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.ShippingAddressResponseDto;
import com.rentaltech.techrental.webapi.customer.service.CustomerService;
import com.rentaltech.techrental.common.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/customer")
@AllArgsConstructor
@Tag(name = "Customers", description = "Customer profile APIs")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "List customers", description = "Retrieve all customers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success")
    })
    public ResponseEntity<?> getAllCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        List<CustomerResponseDto> responseDtos = customers.stream()
                .map(this::mapToResponseDto)
                .toList();
        return ResponseUtil.createSuccessResponse(
                "Lấy danh sách khách hàng thành công",
                "Danh sách khách hàng",
                responseDtos,
                HttpStatus.OK
        );
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Get customer by ID", description = "Retrieve customer by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<?> getCustomerById(@PathVariable Long customerId) {
        Customer customer = customerService.getCustomerByIdOrThrow(customerId);
        return ResponseUtil.createSuccessResponse(
                "Lấy thông tin khách hàng thành công",
                "Chi tiết khách hàng",
                mapToResponseDto(customer),
                HttpStatus.OK
        );
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "My profile", description = "Get current customer's profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> getMyProfile(
            @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseUtil.unauthorized();
        }
        Customer customer = customerService.getCustomerByUsernameOrThrow(principal.getUsername());
        return ResponseUtil.createSuccessResponse(
                "Lấy thông tin hồ sơ thành công",
                "Hồ sơ khách hàng hiện tại",
                mapToResponseDto(customer),
                HttpStatus.OK
        );
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update my profile", description = "Update current customer's profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> updateMyProfile(
            @RequestBody @Valid CustomerUpdateRequestDto request,
            @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseUtil.unauthorized();
        }
        Customer customer = customerService.updateCustomerByUsername(principal.getUsername(), request);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật hồ sơ thành công",
                "Thông tin hồ sơ đã được cập nhật",
                mapToResponseDto(customer),
                HttpStatus.OK
        );
    }

    @PutMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR') or hasRole('TECHNICIAN') or hasRole('CUSTOMER_SUPPORT_STAFF')")
    @Operation(summary = "Update customer", description = "Update customer by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    public ResponseEntity<?> updateCustomer(
            @PathVariable Long customerId,
            @RequestBody @Valid CustomerUpdateRequestDto request) {
        Customer customer = customerService.updateCustomer(customerId, request);
        return ResponseUtil.createSuccessResponse(
                "Cập nhật khách hàng thành công",
                "Thông tin khách hàng đã được cập nhật",
                mapToResponseDto(customer),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete customer", description = "Delete customer by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<?> deleteCustomer(@PathVariable Long customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseUtil.createSuccessResponse(
                "Xóa khách hàng thành công",
                "Khách hàng đã được xóa",
                HttpStatus.NO_CONTENT
        );
    }

    private CustomerResponseDto mapToResponseDto(Customer customer) {
        return CustomerResponseDto.builder()
                .customerId(customer.getCustomerId())
                .accountId(customer.getAccount().getAccountId())
                .username(customer.getAccount().getUsername())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .fullName(customer.getFullName())
                .kycStatus(customer.getKycStatus())
                .shippingAddressDtos(customer.getShippingAddresses().stream().map(
                        entity -> {
                            return ShippingAddressResponseDto.builder()
                                    .shippingAddressId(entity.getShippingAddressId())
                                    .address(entity.getAddress())
                                    .customerId(entity.getCustomer() != null ? entity.getCustomer().getCustomerId() : null)
                                    .createdAt(entity.getCreatedAt())
                                    .updatedAt(entity.getUpdatedAt())
                                    .build();
                        }).toList()

                )
                .bankInformationDtos(customer.getBankInformations().stream().map(
                        entity -> {
                            return BankInformationResponseDto.builder()
                                    .bankInformationId(entity.getBankInformationId())
                                    .bankName(entity.getBankName())
                                    .bankHolder(entity.getBankHolder())
                                    .cardNumber(entity.getCardNumber())
                                    .customerId(entity.getCustomer() != null ? entity.getCustomer().getCustomerId() : null)
                                    .createdAt(entity.getCreatedAt())
                                    .updatedAt(entity.getUpdatedAt())
                                    .build();
                        }).toList()
                )
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}

