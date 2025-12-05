package com.rentaltech.techrental.webapi.customer.service;

import com.rentaltech.techrental.webapi.customer.model.BankInformation;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.dto.BankInformationRequestDto;
import com.rentaltech.techrental.webapi.customer.model.dto.BankInformationResponseDto;
import com.rentaltech.techrental.webapi.customer.repository.BankInformationRepository;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
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
public class BankInformationServiceImpl implements BankInformationService {

    private final BankInformationRepository repository;
    private final CustomerRepository customerRepository;

    public BankInformationServiceImpl(BankInformationRepository repository,
                                      CustomerRepository customerRepository) {
        this.repository = repository;
        this.customerRepository = customerRepository;
    }

    @Override
    public BankInformationResponseDto create(BankInformationRequestDto request) {
        if (request == null) throw new IllegalArgumentException("BankInformationRequestDto không được để trống");
        if (request.getBankName() == null || request.getBankName().isBlank()) {
            throw new IllegalArgumentException("Cần cung cấp tên ngân hàng");
        }
        if (request.getCardNumber() == null || request.getCardNumber().isBlank()) {
            throw new IllegalArgumentException("Cần cung cấp số thẻ");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Customer customer = customerRepository.findByAccount_Username(auth != null ? auth.getName() : null)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng"));
        if (repository.existsByCardNumberAndCustomer_CustomerId(request.getCardNumber(), customer.getCustomerId())) {
            throw new IllegalArgumentException("Thẻ với số này đã được sử dụng");
        }

        BankInformation entity = BankInformation.builder()
                .bankName(request.getBankName())
                .bankHolder(request.getBankHolder())
                .cardNumber(request.getCardNumber())
                .customer(customer)
                .build();
        BankInformation saved = repository.save(entity);
        return BankInformationResponseDto.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BankInformationResponseDto findById(Long id) {
        BankInformation entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thông tin ngân hàng: " + id));
        enforceOwnershipForCustomer(entity.getCustomer() != null ? entity.getCustomer().getCustomerId() : null);
        return BankInformationResponseDto.from(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankInformationResponseDto> findAll() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isCustomer = auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch("ROLE_CUSTOMER"::equals);
        if (isCustomer) {
            var username = auth.getName();
            var customerOpt = customerRepository.findByAccount_Username(username);
            Long requesterCustomerId = customerOpt.map(Customer::getCustomerId).orElse(-1L);
            return repository.findByCustomer_CustomerId(requesterCustomerId).stream()
                    .map(BankInformationResponseDto::from)
                    .toList();
        }
        return repository.findAll().stream().map(BankInformationResponseDto::from).toList();
    }

    @Override
    public BankInformationResponseDto update(Long id, BankInformationRequestDto request) {
        BankInformation existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thông tin ngân hàng: " + id));
        enforceOwnershipForCustomer(existing.getCustomer() != null ? existing.getCustomer().getCustomerId() : null);

        if (request == null) throw new IllegalArgumentException("BankInformationRequestDto không được để trống");
        if (request.getBankName() != null && !request.getBankName().isBlank()) {
            existing.setBankName(request.getBankName());
        }
        if (request.getBankHolder() != null && !request.getBankHolder().isBlank()) {
            existing.setBankHolder(request.getBankHolder());
        }
        if (request.getCardNumber() != null && !request.getCardNumber().isBlank()) {
            existing.setCardNumber(request.getCardNumber());
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Long customerId = customerRepository.findByAccount_Username(username)
                .map(Customer::getCustomerId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khách hàng: " + username));

        if (repository.existsByCardNumberAndCustomer_CustomerId(request.getCardNumber(), customerId)) {
            throw new IllegalArgumentException("Thẻ với số này đã được sử dụng");
        }

        BankInformation saved = repository.save(existing);
        return BankInformationResponseDto.from(saved);
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Không tìm thấy thông tin ngân hàng: " + id);
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
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền: thông tin ngân hàng không thuộc về bạn");
                }
            }
        }
    }

}

