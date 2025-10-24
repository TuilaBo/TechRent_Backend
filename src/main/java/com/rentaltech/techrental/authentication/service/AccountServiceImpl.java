package com.rentaltech.techrental.authentication.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.repository.AccountRepository;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountServiceImpl implements AccountService {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private CustomerRepository customerRepository;

    @Override
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Override
    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findById(id);
    }

    @Override
    public Account addAccount(Account account) {
        // Kiểm tra username đã tồn tại chưa
        if (accountRepository.findByUsername(account.getUsername()) != null) {
            throw new RuntimeException("Username already exists!");
        }
        
        // Kiểm tra email đã tồn tại chưa (chỉ với accounts đã verified)
        if (account.getEmail() != null) {
            Account existingEmailAccount = accountRepository.findByEmail(account.getEmail());
            if (existingEmailAccount != null && existingEmailAccount.getIsActive()) {
                throw new RuntimeException("Email already exists!");
            }
            // Nếu email tồn tại nhưng chưa verified, xóa account cũ
            if (existingEmailAccount != null && !existingEmailAccount.getIsActive()) {
                accountRepository.delete(existingEmailAccount);
            }
        }
        
        // Kiểm tra phone number đã tồn tại chưa (chỉ với accounts đã verified)
        if (account.getPhoneNumber() != null) {
            Account existingPhoneAccount = accountRepository.findByPhoneNumber(account.getPhoneNumber());
            if (existingPhoneAccount != null && existingPhoneAccount.getIsActive()) {
                throw new RuntimeException("Phone number already exists!");
            }
            // Nếu phone tồn tại nhưng chưa verified, xóa account cũ
            if (existingPhoneAccount != null && !existingPhoneAccount.getIsActive()) {
                accountRepository.delete(existingPhoneAccount);
            }
        }
        
        account.setPassword(passwordEncoder.encode(account.getPassword()));
        Account savedAccount = accountRepository.save(account);
        
        // Tự động tạo Customer profile nếu Account có role Customer
        if (savedAccount.getRole() == Role.CUSTOMER) {
            try {
                Customer customer = Customer.builder()
                        .account(savedAccount)
                        .email(savedAccount.getEmail())
                        .phoneNumber(savedAccount.getPhoneNumber())
                        .fullName("Chưa cập nhật") // Giá trị mặc định
                        .build();
                customerRepository.save(customer);
            } catch (Exception e) {
                // Log error nhưng không fail register
                System.err.println("Failed to create customer profile: " + e.getMessage());
            }
        }
        
        return savedAccount;
    }

    @Override
    public void setVerificationCodeAndSendEmail(Account account) {
        String code = String.format("%06d", new java.util.Random().nextInt(1_000_000));
        account.setVerificationCode(code);
        account.setVerificationExpiry(java.time.LocalDateTime.now().plusMinutes(10));
        accountRepository.save(account);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(account.getEmail());
        message.setSubject("Verify your email");
        message.setText("Your verification code is: " + code + "\nThis code expires in 10 minutes.");
        mailSender.send(message);
    }

    @Override
    public boolean verifyEmail(String email, String code) {
        Account account = accountRepository.findByEmailAndVerificationCode(email, code);
        if (account == null) {
            return false;
        }
        if (account.getVerificationExpiry() == null || account.getVerificationExpiry().isBefore(java.time.LocalDateTime.now())) {
            return false;
        }
        account.setIsActive(true);
        account.setVerificationCode(null);
        account.setVerificationExpiry(null);
        accountRepository.save(account);
        return true;
    }

    @Override
    public Optional<Account> getByUsername(String username) {
        return Optional.ofNullable(accountRepository.findByUsername(username));
    }

    @Override
    public Account getByEmail(String email) {
        return accountRepository.findByEmail(email);
    }

    @Override
    public Account updateAccount(Account account) {
        return accountRepository.save(account);
    }

    @Override
    public void deleteAccount(Long accountId) {
        accountRepository.deleteById(accountId);
    }
    
    /**
     * Cleanup các account chưa verify sau một khoảng thời gian
     */
    public void cleanupUnverifiedAccounts() {
        java.time.LocalDateTime cutoffTime = java.time.LocalDateTime.now().minusHours(24); // 24 giờ
        
        List<Account> unverifiedAccounts = accountRepository.findAll().stream()
                .filter(account -> !account.getIsActive() && 
                                 (account.getVerificationExpiry() == null || 
                                  account.getVerificationExpiry().isBefore(cutoffTime)))
                .collect(java.util.stream.Collectors.toList());
        
        for (Account account : unverifiedAccounts) {
            try {
                accountRepository.delete(account);
                System.out.println("Cleaned up unverified account: " + account.getEmail());
            } catch (Exception e) {
                System.err.println("Failed to cleanup account " + account.getEmail() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Resend verification code cho account chưa verify
     */
    public boolean resendVerificationCode(String email) {
        Account account = accountRepository.findByEmail(email);
        if (account == null) {
            return false;
        }
        
        if (account.getIsActive()) {
            return false; // Account đã verified
        }
        
        setVerificationCodeAndSendEmail(account);
        return true;
    }

}
