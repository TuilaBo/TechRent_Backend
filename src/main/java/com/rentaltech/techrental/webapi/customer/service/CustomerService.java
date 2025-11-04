package com.rentaltech.techrental.webapi.customer.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.service.AccountService;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerCreateRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.CustomerUpdateRequestDto;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final AccountService accountService;
    
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerById(Long customerId) {
        return customerRepository.findById(customerId);
    }

    @Transactional(readOnly = true)
    public Customer getCustomerByIdOrThrow(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng với id: " + customerId));
    }
    
    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerByAccountId(Long accountId) {
        return customerRepository.findByAccount_AccountId(accountId);
    }

    @Transactional(readOnly = true)
    public Customer getCustomerByAccountIdOrThrow(Long accountId) {
        return customerRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng cho account id: " + accountId));
    }
    
    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerByUsername(String username) {
        return customerRepository.findByAccount_Username(username);
    }

    @Transactional(readOnly = true)
    public Customer getCustomerByUsernameOrThrow(String username) {
        return customerRepository.findByAccount_Username(username)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng với username: " + username));
    }
    
    public Customer createCustomer(Long accountId, CustomerCreateRequestDto request) {
        // Kiểm tra Account có tồn tại và có role Customer không
        Account account = accountService.getAccountById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản với id: " + accountId));
        
        if (!account.getRole().name().equals("CUSTOMER")) {
            throw new IllegalStateException("Tài khoản không phải khách hàng");
        }
        
        // Kiểm tra Customer đã tồn tại chưa
        if (customerRepository.existsByAccount_AccountId(accountId)) {
            throw new IllegalStateException("Hồ sơ khách hàng đã tồn tại cho tài khoản này");
        }
        
        Customer customer = Customer.builder()
                .account(account)
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .fullName(request.getFullName())
                .shippingAddress(request.getShippingAddress())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankName(request.getBankName())
                .bankAccountHolder(request.getBankAccountHolder())
                .build();
        
        return customerRepository.save(customer);
    }
    
    public Customer updateCustomer(Long customerId, CustomerUpdateRequestDto request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng với id: " + customerId));
        
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setFullName(request.getFullName());
//        customer.setShippingAddress(request.getShippingAddress());
//        customer.setBankAccountNumber(request.getBankAccountNumber());
        customer.setBankName(request.getBankName());
        customer.setBankAccountHolder(request.getBankAccountHolder());
        
        return customerRepository.save(customer);
    }
    
    public Customer updateCustomerByAccountId(Long accountId, CustomerUpdateRequestDto request) {
        Customer customer = customerRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng cho account id: " + accountId));
        
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setFullName(request.getFullName());
//        customer.setShippingAddress(request.getShippingAddress());
//        customer.setBankAccountNumber(request.getBankAccountNumber());
        customer.setBankName(request.getBankName());
        customer.setBankAccountHolder(request.getBankAccountHolder());
        
        return customerRepository.save(customer);
    }
    
    public void deleteCustomer(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new NoSuchElementException("Không tìm thấy khách hàng với id: " + customerId);
        }
        customerRepository.deleteById(customerId);
    }

    public Customer updateCustomerByUsername(String username, CustomerUpdateRequestDto request) {
        Long accountId = accountService.getByUsername(username)
                .map(Account::getAccountId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản với username: " + username));
        return updateCustomerByAccountId(accountId, request);
    }

    public void updateCustomerFcmToken(String username, String token) {
        Customer customer = getCustomerByUsernameOrThrow(username);
        customer.setFcmToken(token);
        customerRepository.save(customer);
    }
}
