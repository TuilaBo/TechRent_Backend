package com.rentaltech.techrental.authentication.service;

import com.rentaltech.techrental.authentication.model.Account;
import com.rentaltech.techrental.authentication.model.dto.CreateUserRequestDto;

import java.util.List;
import java.util.Optional;

public interface AccountService {
    List<Account> getAllAccounts();
    Optional<Account> getAccountById(Long id);
    Account addAccount(Account account);

    // Register a new CUSTOMER account and send verification email
    Account registerCustomer(CreateUserRequestDto request);

    void setVerificationCodeAndSendEmail(Account account);
    // Throws IllegalArgumentException if invalid/expired, NoSuchElementException if not found
    void verifyEmail(String email, String code);

    Optional<Account> getByUsername(String username);

    Account getByEmail(String email);

    Account updateAccount(Account account);

    void deleteAccount(Long accountId);
    
    /**
     * Cleanup các account chưa verify sau một khoảng thời gian
     */
    void cleanupUnverifiedAccounts();
    
    /**
     * Resend verification code cho account chưa verify
     */
    // Throws IllegalArgumentException/NoSuchElementException when invalid
    void resendVerificationCode(String email);

    /**
     * Authenticate user with username or email and return JWT token
     */
    String authenticateAndGenerateToken(com.rentaltech.techrental.authentication.model.dto.LoginDto loginDto);

    /**
     * Send reset password code to email
     */
    void forgotPassword(String email);

    /**
     * Reset password using code from email
     */
    void resetPassword(String email, String code, String newPassword);
}
