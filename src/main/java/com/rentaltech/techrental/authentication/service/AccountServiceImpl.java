package com.rentaltech.techrental.authentication.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.Role;
import com.rentaltech.techrental.authentication.model.dto.CreateUserRequestDto;
import com.rentaltech.techrental.authentication.model.dto.LoginDto;
import com.rentaltech.techrental.authentication.repository.AccountRepository;
import com.rentaltech.techrental.security.JwtTokenProvider;
import com.rentaltech.techrental.webapi.customer.model.Customer;
import com.rentaltech.techrental.webapi.customer.model.CustomerStatus;
import com.rentaltech.techrental.webapi.customer.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class AccountServiceImpl implements AccountService {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private VerificationEmailService verificationEmailService;

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
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }
        
        // Kiểm tra email đã tồn tại chưa (chỉ với accounts đã verified)
        if (account.getEmail() != null) {
            Account existingEmailAccount = accountRepository.findByEmail(account.getEmail());
            if (existingEmailAccount != null && existingEmailAccount.getIsActive()) {
                throw new IllegalArgumentException("Email đã tồn tại!");
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
                throw new IllegalArgumentException("Số điện thoại đã tồn tại!");
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
                // Set status INACTIVE nếu account chưa được verify
                CustomerStatus customerStatus = Boolean.TRUE.equals(savedAccount.getIsActive()) 
                    ? CustomerStatus.ACTIVE 
                    : CustomerStatus.INACTIVE;
                
                Customer customer = Customer.builder()
                        .account(savedAccount)
                        .email(savedAccount.getEmail())
                        .phoneNumber(savedAccount.getPhoneNumber())
                        .fullName("Chưa cập nhật") // Giá trị mặc định
                        .status(customerStatus) // Set status dựa trên isActive của account
                        .build();
                customerRepository.save(customer);
            } catch (Exception e) {
                // Log error nhưng không fail register
                System.err.println("Không thể tạo hồ sơ khách hàng: " + e.getMessage());
            }
        }
        
        return savedAccount;
    }

    @Override
    public Account registerCustomer(CreateUserRequestDto request) {
        Account account = Account.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .isActive(false)
                .role(Role.CUSTOMER)
                .build();
        Account saved = addAccount(account);
        setVerificationCodeAndSendEmail(saved);
        return saved;
    }

    @Override
    public void setVerificationCodeAndSendEmail(Account account) {
        String code = String.format("%06d", new java.util.Random().nextInt(1_000_000));
        account.setVerificationCode(code);
        account.setVerificationExpiry(java.time.LocalDateTime.now().plusMinutes(10));
        accountRepository.save(account);
        // Send email asynchronously to avoid blocking response
        verificationEmailService.sendVerificationEmail(account.getEmail(), code);
    }

    @Override
    public void verifyEmail(String email, String code) {
        Account account = accountRepository.findByEmailAndVerificationCode(email, code);
        if (account == null) {
            throw new NoSuchElementException("Không tìm thấy tài khoản hoặc mã xác thực không đúng");
        }
        if (account.getVerificationExpiry() == null || account.getVerificationExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã xác thực không hợp lệ hoặc đã hết hạn");
        }
        account.setIsActive(true);
        account.setVerificationCode(null);
        account.setVerificationExpiry(null);
        accountRepository.save(account);
        
        // Update Customer status thành ACTIVE khi verify email thành công
        if (account.getRole() == Role.CUSTOMER) {
            try {
                Customer customer = customerRepository.findByAccount_AccountId(account.getAccountId())
                        .orElse(null);
                if (customer != null) {
                    customer.setStatus(CustomerStatus.ACTIVE);
                    customerRepository.save(customer);
                }
            } catch (Exception e) {
                System.err.println("Không thể cập nhật trạng thái Customer: " + e.getMessage());
            }
        }
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
                System.out.println("Đã dọn dẹp tài khoản chưa xác thực: " + account.getEmail());
            } catch (Exception e) {
                System.err.println("Không thể dọn dẹp tài khoản " + account.getEmail() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Resend verification code cho account chưa verify
     */
    @Override
    public void resendVerificationCode(String email) {
        Account account = accountRepository.findByEmail(email);
        if (account == null) {
            throw new NoSuchElementException("Không tìm thấy tài khoản với email đã cung cấp");
        }
        if (Boolean.TRUE.equals(account.getIsActive())) {
            throw new IllegalStateException("Tài khoản đã được xác thực");
        }
        setVerificationCodeAndSendEmail(account);
    }

    @Override
    public String authenticateAndGenerateToken(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getUsernameOrEmail(),
                        loginDto.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return tokenProvider.generateToken(authentication);
    }

    @Override
    public void forgotPassword(String email) {
        Account account = accountRepository.findByEmail(email);
        if (account == null) {
            // Không tiết lộ email có tồn tại hay không vì lý do bảo mật
            return;
        }
        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new IllegalStateException("Tài khoản chưa được kích hoạt. Vui lòng xác thực email trước.");
        }
        setResetPasswordCodeAndSendEmail(account);
    }

    @Override
    public void resetPassword(String email, String code, String newPassword) {
        Account account = accountRepository.findByEmailAndResetPasswordCode(email, code);
        if (account == null) {
            throw new NoSuchElementException("Không tìm thấy tài khoản hoặc mã đặt lại mật khẩu không đúng");
        }
        if (account.getResetPasswordExpiry() == null || account.getResetPasswordExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
        }
        // Encode password mới bằng BCrypt
        account.setPassword(passwordEncoder.encode(newPassword));
        account.setResetPasswordCode(null);
        account.setResetPasswordExpiry(null);
        accountRepository.save(account);
    }

    private void setResetPasswordCodeAndSendEmail(Account account) {
        String code = String.format("%06d", new java.util.Random().nextInt(1_000_000));
        account.setResetPasswordCode(code);
        account.setResetPasswordExpiry(java.time.LocalDateTime.now().plusMinutes(10));
        accountRepository.save(account);
        // Send email asynchronously to avoid blocking response
        verificationEmailService.sendResetPasswordEmail(account.getEmail(), code);
    }

}
